package excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import sadeh.ActicalEpoch;

public class ActicalExcelParser {
	// The index of the row with the time when the data point was recorded
	private static final int EPOCH_TIME_INDEX = 0;
	// Index of the header row containing the data column headers
	private static final int HEADER_ROW_INDEX = 2;
	// The data doesn't start immediately after the header row
	private static final int BEGIN_DATA_ROW_INDEX = 15;
	// Times are formatted similarly to military time e.g. 23:59:00
	private static final SimpleDateFormat acticalTimeFormat = new SimpleDateFormat("HH:mm:ss");
	// Workbook name where the data is stored
	private static final String actigraphWorkbook = "Data from ActiCal";

	/**
	 * Parses an excel document containing Actigraph data representing sleep
	 * activity for a participant. Each document only contains data about a
	 * single participant but will contain multiple columns, one for each day
	 * when data was collected. The data is an integer representing activity
	 * level and it is collected each minute, one excel row for each minute of
	 * the day.
	 *
	 * @param path
	 *            Path to the Excel document
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public static List<ActicalEpoch> parseSadehExcelDocument(File excel)
			throws ParticipantDataParseException {
		List<ActicalEpoch> epochs = new ArrayList<>();
		XSSFWorkbook wb = null;
		
		// Parse the sleep data from the body rows of the excel document
		XSSFRow row = null;
		String time = null; // the time an epoch of activity data was collected
		int rowIdx = BEGIN_DATA_ROW_INDEX;
		int totalEpochs = 0;

		try {
			FileInputStream fis = new FileInputStream(excel);
			wb = new XSSFWorkbook(fis);
			XSSFSheet ws = wb.getSheet(actigraphWorkbook);
			
			// The epoch data is in non-contiguous columns with known names, this finds the columns indices.
			List<ActigraphDataHeader> headers = parseHeader(ws);
			
			do {
				row = ws.getRow(rowIdx);

				if (row != null) {
					time = parseTimeActivityRecorded(row);

					if (time != null) {

						for (ActigraphDataHeader header : headers) {
							XSSFCell cell = row.getCell(header.getColumnIndex());
							if (!isCellEmpty(cell)) {
								String dataCollectionDay = header.getDayOfWeek();
								int activityLevel = (int) cell.getNumericCellValue();
								ActicalEpoch epoch = new ActicalEpoch();
								epoch.setActivityLevel(activityLevel);
								epoch.setDayOfWeek(dataCollectionDay);

								LocalDate ld = header.getDate();
								LocalDateTime epochTime = getLocalDateTime(ld, time);
								epoch.setDateTime(epochTime);
								epoch.setDate(ld);
								epochs.add(epoch);
								totalEpochs++;
							}
						}
					}
				}
				rowIdx++;
			} while (row != null && time != null);

		} catch (FileNotFoundException ex) {
			throw new ParticipantDataParseException("File " + excel.getAbsolutePath()
			+ " cannot be opened, it must be manually processed.");
		} catch (IOException io) {
			throw new ParticipantDataParseException("IO error occurred processing the file " + excel.getAbsolutePath()
			+ ", it must be manually processed.");
		} finally {
			if (wb != null)
				try {
					wb.close();
				} catch (IOException e) {
					throw new ParticipantDataParseException("IO error occurred processing the file " + excel.getAbsolutePath()
						+ ", it must be manually processed.");
				}
		}

		System.out.println("Total epochs in document: " + totalEpochs);
		return epochs;
	}

	private static LocalDateTime getLocalDateTime(LocalDate ld, String actigraphTime) {
		try {
			String[] ata = actigraphTime.split(":");
			int hour = Integer.parseInt(ata[0]);
			int minute = Integer.parseInt(ata[1]);
			int second = Integer.parseInt(ata[2]);
			return ld.atTime(hour, minute, second);
		} catch (Exception e) {
		}

		return null;
	}

	@SuppressWarnings("deprecation")
	public static boolean isCellEmpty(final XSSFCell cell) {
		if (cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
			return true;
		}

		if (cell.getCellType() == Cell.CELL_TYPE_STRING && cell.getStringCellValue().isEmpty()) {
			return true;
		}

		return false;
	}

	/**
	 * Each row contains a time in the format HH:mm:ss (participant data is
	 * collected once per minute). This parses the value from the Excel
	 * document.
	 * 
	 * @param row
	 * @return
	 */
	private static String parseTimeActivityRecorded(XSSFRow row) {
		XSSFCell cell = row.getCell(EPOCH_TIME_INDEX);
		Date date = parseDate(cell);

		if (date != null) {
			return acticalTimeFormat.format(date);
		} else {
			return null;
		}
	}

	private static Date parseDate(XSSFCell cell) {
		if (!isCellEmpty(cell)) {
			return cell.getDateCellValue();
		} else {
			return null;
		}
	}

	private static List<ActigraphDataHeader> parseHeader(XSSFSheet ws) {
		List<ActigraphDataHeader> headers = new ArrayList<>(8); // There should
																// be no more
																// than 8
																// columns
		XSSFRow row = ws.getRow(HEADER_ROW_INDEX);
		XSSFRow dateRow = ws.getRow(BEGIN_DATA_ROW_INDEX);

		int maxColIdx = 26; // There cannot be headers past this column

		for (int i = 0; i < maxColIdx; i++) {
			XSSFCell cell = row.getCell(i);
			if (cell != null) {
				try {
					String value = cell.getStringCellValue();
					if (value != null && !value.equalsIgnoreCase("") && getHeaderName(value) != null) {
						ActigraphDataHeader header = new ActigraphDataHeader();

						if (i - 2 >= 1) {
							Date date = parseDate(dateRow.getCell(i - 2));
							LocalDate ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
							header.setDate(ld);
						} else {
							throw new Exception("Date cannot be found for header column " + i);
						}

						header.setColumnIndex(i);
						header.setHeader(value);
						header.setDayOfWeek(getHeaderName(value));
						headers.add(header);
					}
				} catch (Exception e) {
				}
			}
		}

		return headers;
	}

	/**
	 * If the name is a day of the week, returns it in standard format,
	 * otherwise, returns null.
	 */
	public static String getHeaderName(String name) {
		String temp = name.toLowerCase();
		if (temp.contains("mon")) {
			return "Monday";
		} else if (temp.contains("tue")) {
			return "Tuesday";
		} else if (temp.contains("wed")) {
			return "Wednesday";
		} else if (temp.contains("thu")) {
			return "Thursday";
		} else if (temp.contains("fri")) {
			return "Friday";
		} else if (temp.contains("sat")) {
			return "Saturday";
		} else if (temp.contains("sun")) {
			return "Sunday";
		}

		return null;
	}

	/**
	 * The Actigraphy data is arranged into columns where the first column is
	 * the minute of day, the second column is Monday, the third Tuesday, etc.
	 * Under Monday, Tuesday, and the other days of the week, the value at each
	 * cell is the activity level of the participant for that time of day.
	 * 
	 * We're collecting data for each day of the week so this helps us know what
	 * day of the week we're parsing data for.
	 * 
	 * @author kyle_
	 *
	 */
	public static class ActigraphDataHeader {
		String header;
		String dayOfWeek;
		int columnIndex;
		LocalDate date;

		public LocalDate getDate() {
			return date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}

		public String getHeader() {
			return header;
		}

		public void setHeader(String header) {
			this.header = header;
		}

		public String getDayOfWeek() {
			return dayOfWeek;
		}

		public void setDayOfWeek(String dayOfWeek) {
			this.dayOfWeek = dayOfWeek;
		}

		public int getColumnIndex() {
			return columnIndex;
		}

		public void setColumnIndex(int columnIndex) {
			this.columnIndex = columnIndex;
		}
	}
}
