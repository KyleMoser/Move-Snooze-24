package excel;

import ema.EMAPrompt;
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
import java.util.Arrays;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/**
 * Parses an Ecological Momentary Assessment (EMA) Excel document which contains data about a child's sleep status
 * at a particular minute of the day. The data is dependent on a survey taken by the child's mother, so the survey
 * data may be incomplete as the mother does not always respond to survey questions. The data is coded as '1' for 
 * awake and '2' for asleep.
 * 
 * @author kyle_
 *
 */
public class EMAExcelParser {
	public static final String ID_HEADER = "ID";
	public static final String DATE_HEADER = "DATE_IN";
	public static final String TIME_HEADER = "TIME_IN";
	public static final String CSLEEP_HEADER = "CSLEEP";
	public static final List<String> excelHeaders = new ArrayList<>(
			Arrays.asList(ID_HEADER, DATE_HEADER, TIME_HEADER, CSLEEP_HEADER));
	// Times are formatted similarly to military time e.g. 23:59:00
	private static final SimpleDateFormat milTimeFormat = new SimpleDateFormat("HH:mm:ss");
	
	/**
	 * Parses an excel document containing EMA data representing sleep
	 * activity for a participant. 
	 *
	 * @param path
	 *            Path to the Excel document
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public static List<EMAPrompt> parseEcologicalMomentaryAssessment(File excel)
			throws ParticipantDataParseException {
		List<EMAPrompt> prompts = new ArrayList<>();
		XSSFWorkbook wb = null;
		
		// Parse the sleep data from the body rows of the excel document
		XSSFRow row = null;
		int rowIdx = 1; //Data always starts on the second row

		try {
			FileInputStream fis = new FileInputStream(excel);
			System.out.println("Opened FIS");
			wb = new XSSFWorkbook(fis);
			System.out.println("Opened workbook");
			XSSFSheet ws = wb.getSheetAt(0);
			System.out.println("Opened worksheet");
			
			// The epoch data is in columns with known names, this finds the columns indices.
			List<EMAHeader> headers = parseHeader(ws);
			boolean moreRows = true;
			
			do {
				String participantID = null;
				LocalDateTime promptTime = null;
				LocalDate localDate = null;
				String time = null;
				boolean asleep = false;
				int csleep = -1; // no response from participant
				boolean hasResponse = false;
				row = ws.getRow(rowIdx);

				if (row != null) {
					for (EMAHeader header : headers) {
						XSSFCell cell = row.getCell(header.getColumnIndex());
						if (!isCellEmpty(cell)) {
							switch (header.getHeaderFieldName()){
							case ID_HEADER:
								//System.out.println("Excel row: " + (rowIdx+1) + ", participant ID " + cell.getNumericCellValue());
								participantID = String.valueOf((int)cell.getNumericCellValue());
								break;
							case DATE_HEADER:
								Date date = parseDate(cell);
								localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
								break;
							case TIME_HEADER:
								time = parseTime(cell);
								break;
							case CSLEEP_HEADER:
								csleep = (int)cell.getNumericCellValue();
								if (csleep == 1){
									hasResponse = true;
								} else if (csleep == 2){
									hasResponse = true;
									asleep = true;
								}
								
								break;
							}
							
							promptTime = getLocalDateTime(localDate, time);
						}
					}
					
					if (localDate == null || participantID == null || time == null){
						moreRows = false;
						System.out.println("No data found at EXCEL row " + (rowIdx+1));
					} else {
						EMAPrompt prompt = new EMAPrompt();
						prompt.setAsleep(asleep);
						prompt.setResponsed(hasResponse);
						prompt.setDateTime(promptTime);
						prompt.setParticipant(participantID);
						prompts.add(prompt);
					}
				}
				rowIdx++;
			} while (row != null && moreRows);

		} catch (FileNotFoundException ex) {
			throw new ParticipantDataParseException("File " + excel.getAbsolutePath()
			+ " cannot be opened, it must be manually processed.");
		} catch (IOException io) {
			System.out.println("Exception opening workbook " + io.getMessage());
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

		System.out.println("Total EMAPrompts in document: " + prompts.size() + ", last excel data row: " + (rowIdx-1));
		return prompts;
	}

	/**
	 * cell contains a time in the format HH:mm:ss 
	 * 
	 * @param cell
	 * @return time as string
	 */
	private static String parseTime(XSSFCell cell) {
		Date date = parseDate(cell);

		if (date != null) {
			return milTimeFormat.format(date);
		} else {
			return null;
		}
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

	private static Date parseDate(XSSFCell cell) {
		if (!isCellEmpty(cell)) {
			return cell.getDateCellValue();
		} else {
			return null;
		}
	}

	private static List<EMAHeader> parseHeader(XSSFSheet ws) {
		List<EMAHeader> headers = new ArrayList<>(4); 
		XSSFRow row = ws.getRow(0);

		int maxColIdx = 26; // There cannot be headers past this column

		for (int i = 0; i < maxColIdx; i++) {
			XSSFCell cell = row.getCell(i);
			if (cell != null) {
				try {
					String value = cell.getStringCellValue();
					if (value != null && !value.equalsIgnoreCase("") && excelHeaders.contains(value)) {
						EMAHeader header = new EMAHeader();
						header.setColumnIndex(i);
						header.setHeaderFieldName(value);
						headers.add(header);
					}
				} catch (Exception e) {
				}
			}
		}

		return headers;
	}

	/**
	 * The EMA data is arranged into columns for participant ID, date, time and sleep status.
	 * 
	 * @author kyle_
	 *
	 */
	public static class EMAHeader {
		String headerFieldName;
		int columnIndex;
		
		public static EMAHeader getHeader(String name, List<EMAHeader> headers){
			for (EMAHeader h : headers){
				if (h.getHeaderFieldName().equalsIgnoreCase(name))
					return h;
			}
			
			return null;
		}
		
		public String getHeaderFieldName() {
			return headerFieldName;
		}

		public void setHeaderFieldName(String headerFieldName) {
			this.headerFieldName = headerFieldName;
		}

		public int getColumnIndex() {
			return columnIndex;
		}

		public void setColumnIndex(int columnIndex) {
			this.columnIndex = columnIndex;
		}
	}
}
