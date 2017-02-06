package excel;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import sadeh.ActicalEpoch;

public class ActivityThresholdWorkbook {
	public static final String worksheetName = "Activity Threshold";
	public String outputLocation = null;
	public Workbook workbook = null;
	public String participant = null;
	public List<ActicalEpoch> epochs = null;
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
	DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
	
	public ActivityThresholdWorkbook(String outputFile, String participant, List<ActicalEpoch> epochs){
		this.outputLocation = outputFile;
		this.epochs = epochs;
		this.participant = participant;
	}
	
	public void create() throws ActicalDataOutputException{
		workbook = new HSSFWorkbook(); 
	    Sheet worksheet = workbook.createSheet(worksheetName);
	    SortedSet <LocalDate> epochDates = new TreeSet<>();
	    //HashMap<String, Integer> dateToCellIndex = new HashMap<>();
	    Row dateRow = worksheet.createRow(0);
	    Row dayOfWeekRow = worksheet.createRow(1);
	    HashMap<LocalDate, Integer> dateToCellIdx = new HashMap<>();
	    
	    HashMap<String, Row> hourMinuteRow = new HashMap<>(); 
	    int rowIdx = 2; //Row 0 and 1 are header rows that says the date and day of the week
	    
	    for (ActicalEpoch epoch : epochs){
	    	LocalDate date = epoch.getDate();
	    	if (!epochDates.contains(date)) 
	    		epochDates.add(date);
	    }
	    
	    int dateCellIdx = 1;
	    for (LocalDate date : epochDates){
	    	dateToCellIdx.put(date, dateCellIdx++);
	    }
	    
	    for (ActicalEpoch epoch : epochs){
	    	LocalDateTime ldt = epoch.getDateTime();
	    	String hourMinute = ldt.format(formatter);
	    	String date = ldt.format(dateFormatter);
	    	
	    	if (!hourMinuteRow.containsKey(hourMinute)){
	    		Row r = createActivityThresholdRow(worksheet, hourMinute, rowIdx++);
	    		hourMinuteRow.put(hourMinute, r);
	    	}
	    	
	    	String day = ldt.getDayOfWeek().toString();
	    }

	    try{
		    FileOutputStream fileOut = new FileOutputStream(outputLocation);
		    workbook.write(fileOut);
		    fileOut.close();
	    } catch(Exception e){
	    	throw new ActicalDataOutputException("Unable to create activity threshold workbook for participant "
	    			+ participant + " at path " + outputLocation);
	    }
	}
	
	public Row createActivityThresholdRow(Sheet sheet, String hourMinute, int rowIdx){
		Row row = sheet.createRow(rowIdx);
	    Cell cell = row.createCell(0);
	    cell.setCellValue(hourMinute);
	    return row;
	}
		
}
