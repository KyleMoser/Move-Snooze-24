package excel;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
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

import analysis.SleepStats;
import sadeh.ActicalEpoch;
import sadeh.ActicalParticipant;
import sadeh.NapData;

public class ParticipantWorkbook {
	public static final String worksheetName = "Participant Data";
	public Workbook workbook = null;
	public List<ActicalParticipant> participants = null;
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
	DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
	CreationHelper createHelper = null;
	
	public ParticipantWorkbook(List<ActicalParticipant> participants){
		this.participants = participants;
	}
	
	public void createHeader(Row header, String[] cols){
		for (int i = 0; i < cols.length; i++){
			Cell c = header.createCell(i);
			c.setCellValue(cols[i]);
		}
	}
	
	public void create() throws ActicalDataOutputException{
		try {
			workbook = new XSSFWorkbook(); 
			createHelper = workbook.getCreationHelper();
			String sheetName = WorkbookUtil.createSafeSheetName(worksheetName);
		    Sheet worksheet = workbook.createSheet(sheetName);
	
		    Row header = worksheet.createRow(0);
		    String[] cols = {"ID", "Day", "Date", "Day_of_week", "Number_Naps", "Average_Nap_Duration", 
		    		"Min_Nap_Duration", "Max_Nap_Duration", "Sleep_Onset_Time", 
		    		"Sleep_Offset_Time", "Night_Sleep_Period", "TST", "TWT", "Sleep_Efficiency", 
		    		"Percent_24hr_Sleep", "Sedentary_PA", "Light_PA", "MVPA", "Eight_to_Eight"};
		    createHeader(header, cols);
		   	int rowIdx = 1; //Row 0 is a header row 
		    
		    for (ActicalParticipant p : participants){
		    	Set<String> dates = p.getDateEpochMap().keySet();
		    	TreeSet<LocalDate> ldSet = new TreeSet<LocalDate>();
		    	for (String date : dates){
		    		LocalDate ld = LocalDate.parse(date, dateFormatter);
		    		ldSet.add(ld);
		    	}
		   
		    	int day = 1;
		    	for (LocalDate current : ldSet){
		    		createParticipantRow(worksheet, day++, current, p, rowIdx++);
		    	}
		    }
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	static class SleepDataSorter implements Comparator<SleepStats>{
		@Override
		public int compare(SleepStats stats1, SleepStats stats2) {
			return stats1.getLocalDate().compareTo(stats2.getLocalDate());
		}
	}
	
	public void write(String output) throws ActicalDataOutputException{
		try{
		    FileOutputStream fileOut = new FileOutputStream(output);
		    workbook.write(fileOut);
		    fileOut.close();
	    } catch(Exception e){
	    	throw new ActicalDataOutputException("Unable to create participant workbook ");
	    }
	}
	
	public Row createParticipantRow(Sheet sheet, int day, LocalDate ld, ActicalParticipant p, int rowIdx){
		String ldStr = ld.format(dateFormatter);
		int col = 0;
		
		Row row = sheet.createRow(rowIdx);
	    Cell cell1 = row.createCell(col++);
	    cell1.setCellValue(p.getParticipant());
	    Cell cell2 = row.createCell(col++);
	    cell2.setCellValue(day);
	    Cell cell3 = row.createCell(col++);
	    cell3.setCellValue(ldStr);
	    
	    Cell dateCell = row.createCell(col++);
	    dateCell.setCellValue(ld.getDayOfWeek().toString());
	    
	    //Nap section of workbook stats
	    NapData napStats = p.getNapMap().get(ldStr);
	    Cell cell4 = row.createCell(col++);
	    cell4.setCellValue(napStats.getNumberNaps());
	    Cell cell5 = row.createCell(col++);
	    cell5.setCellValue(napStats.getAverageNap());
	    Cell cell6 = row.createCell(col++);
	    cell6.setCellValue(napStats.getMinNap());
	    Cell cell7= row.createCell(col++);
	    cell7.setCellValue(napStats.getMaxNap());
	    
	    //Sleep section of workbook stats
	    
	    //Sleep onset
	    SleepStats sleepStats = p.getSleepStats().get(ldStr);
	    LocalDateTime sleepOnset = sleepStats.getSleepOnset();
	    LocalDateTime sleepOffset = sleepStats.getSleepOffset();
	    
    	Cell cell8 = row.createCell(col++);
    	CellStyle cellStyle = workbook.createCellStyle();
    	cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("hh:mm"));
    	if (sleepOnset != null){
    		Date jDate = Date.from(sleepOnset.atZone(ZoneId.systemDefault()).toInstant());
    		cell8.setCellValue(jDate);
    	} else{
    		Date jDate = null;
    		cell8.setCellValue(jDate);
    	}
    	cell8.setCellStyle(cellStyle);
    	
    	//Sleep offset
    	Cell cell9 = row.createCell(col++);
    	CellStyle cellStyle2 = workbook.createCellStyle();
    	cellStyle2.setDataFormat(createHelper.createDataFormat().getFormat("hh:mm"));
    	if (sleepOffset != null){
    		Date jDate2 = Date.from(sleepOffset.atZone(ZoneId.systemDefault()).toInstant());
    		cell9.setCellValue(jDate2);
    	} else{
    		Date jDate2 = null;
    		cell9.setCellValue(jDate2);
    	}
    	cell9.setCellStyle(cellStyle2);
    	
    	Cell cell10 = row.createCell(col++);
    	if (sleepOnset != null && sleepOffset != null){
    		cell10.setCellValue(sleepStats.getNightSleepPeriod());
    	} else{
    		cell10.setCellValue("");
    	}
    	
    	Cell cell11 = row.createCell(col++);
    	if (sleepOnset != null && sleepOffset != null){
    		cell11.setCellValue(sleepStats.getTotalSleepTime());
    	} else{
    		cell11.setCellValue("");
    	}
    	
    	Cell cell12 = row.createCell(col++);
    	if (sleepOnset != null && sleepOffset != null){
    		cell12.setCellValue(sleepStats.getTotalWakeTime());
    	} else{
    		cell12.setCellValue("");
    	}
    	Cell cell13 = row.createCell(col++);
    	if (sleepOnset != null && sleepOffset != null){
    		cell13.setCellValue(sleepStats.getSleepEfficiency());
    	} else{
    		cell13.setCellValue("");
    	}
    	
    	Cell cell14 = row.createCell(col++);
    	cell14.setCellValue(sleepStats.getPercentDailySleep());
    	
    	Cell cell15 = row.createCell(col++);
    	cell15.setCellValue(sleepStats.getSedentary());
    	
    	Cell cell16 = row.createCell(col++);
    	cell16.setCellValue(sleepStats.getLight());
    	
    	Cell cell17 = row.createCell(col++);
    	cell17.setCellValue(sleepStats.getMvpa());
    	
    	Cell cell18 = row.createCell(col++);
    	cell18.setCellValue(sleepStats.getEightToEight());
	    
	    return row;
	}
}
