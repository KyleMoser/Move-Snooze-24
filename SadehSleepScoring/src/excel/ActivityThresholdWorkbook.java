package excel;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import sadeh.ActicalEpoch;

public class ActivityThresholdWorkbook {
	public static final String worksheetName = "Activity Threshold";
	public Workbook workbook = null;
	public String participant = null;
	public List<ActicalEpoch> epochs = null;
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
	DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
	
	public ActivityThresholdWorkbook(List<ActicalEpoch> epochs){
		this.epochs = epochs;
	}
	
	public void create() throws ActicalDataOutputException{
		workbook = new XSSFWorkbook(); 
		CreationHelper createHelper = workbook.getCreationHelper();
		String sheetName = WorkbookUtil.createSafeSheetName(worksheetName);
	    Sheet worksheet = workbook.createSheet(sheetName);
	    SortedSet <LocalDate> epochDates = new TreeSet<>();
	    //HashMap<String, Integer> dateToCellIndex = new HashMap<>();
	    Row dateRow = worksheet.createRow(0);
	    Cell epochDateCell = dateRow.createCell(0);
	    epochDateCell.setCellValue("Time");

	    HashMap<LocalDate, Integer> dateToCellIdx = new HashMap<>();
	   
	    HashMap<String, Row> hourMinuteRow = new HashMap<>(); 
	    int rowIdx = 1; //Row 0 is a header row that says the date
	    
	    for (ActicalEpoch epoch : epochs){
	    	LocalDate date = epoch.getDate();
	    	if (!epochDates.contains(date)) 
	    		epochDates.add(date);
	    }
	    
	    int dateCellIdx = 1;
	    for (LocalDate date : epochDates){
	    	dateToCellIdx.put(date, dateCellIdx);
	    	
	    	//Create date cell
	    	Cell dateCell = dateRow.createCell(dateCellIdx);
	    	CellStyle cellStyle = workbook.createCellStyle();
	    	cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("m/d/yy"));
	    	Date jDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
	    	dateCell.setCellValue(jDate);
	    	dateCell.setCellStyle(cellStyle);
	    	
	    	dateCellIdx++;
	    }
	    
	    for (ActicalEpoch epoch : epochs){
	    	LocalDateTime ldt = epoch.getDateTime();
	    	String hourMinute = ldt.format(formatter);
	    	String date = ldt.format(dateFormatter);
	    	
	    	if (!hourMinuteRow.containsKey(hourMinute)){
	    		Row r = createActivityThresholdRow(worksheet, hourMinute, rowIdx++);
	    		hourMinuteRow.put(hourMinute, r);
	    	}
	    }
	    
	    //Insert the data into the spreadsheet; we already created all of the rows
	    //for each epoch as well as all of the headers
	    for (ActicalEpoch epoch : epochs){
	    	LocalDateTime ldt = epoch.getDateTime();
	    	String hourMinute = ldt.format(formatter);
	    	Row row = hourMinuteRow.get(hourMinute);
	    	
	    	if (row == null){
	    		throw new ActicalDataOutputException("Cannot find excel row for the epoch " + hourMinute);
	    	}
	    	
	    	Integer cellIdx = dateToCellIdx.get(epoch.getDate());
	    	
	    	if (cellIdx == null){
	    		throw new ActicalDataOutputException("Cannot find excel cell index for date " + epoch.getDate());
	    	}
	    	
	    	Cell dataCell = row.createCell(cellIdx);
	    	dataCell.setCellValue(epoch.getActivityThreshold().ordinal());
	    }
	}
	
	public void write(String output) throws ActicalDataOutputException{
		try{
		    FileOutputStream fileOut = new FileOutputStream(output);
		    workbook.write(fileOut);
		    fileOut.close();
	    } catch(Exception e){
	    	throw new ActicalDataOutputException("Unable to create activity threshold workbook for participant "
	    			+ participant + " at path " + output);
	    }
	}
	
	public Row createActivityThresholdRow(Sheet sheet, String hourMinute, int rowIdx){
		Row row = sheet.createRow(rowIdx);
	    Cell cell = row.createCell(0);
	    cell.setCellValue(hourMinute);
	    return row;
	}
		
}
