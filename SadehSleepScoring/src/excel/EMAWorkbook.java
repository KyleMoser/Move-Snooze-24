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
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ema.*;
import analysis.SleepStats;
import sadeh.ActicalEpoch;
import sadeh.ActicalParticipant;
import sadeh.NapData;

public class EMAWorkbook {
	public static final String worksheetName = "EMA Data";
	public Workbook workbook = null;
	public List<EMAResult> emaResults = null;
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
	DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
	static DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
	CreationHelper createHelper = null;
	
	public EMAWorkbook(List<EMAResult> emaResults){
		this.emaResults = emaResults;
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
		    String[] cols = {"ID", "EMA prompt datetime", "EMA prompt asleep", "Actical Epoch #", "Actical epoch datetime", "Actical sleep"};
		    createHeader(header, cols);
		   	int rowIdx = 1; //Row 0 is a header row 
		   			    
		    for (EMAResult ema : emaResults){
		    	/*Set<String> dates = p.getDateEpochMap().keySet();
		    	TreeSet<LocalDate> ldSet = new TreeSet<LocalDate>();
		    	for (String date : dates){
		    		LocalDate ld = LocalDate.parse(date, dateFormatter);
		    		ldSet.add(ld);
		    	}
		   
		    	int day = 1;
		    	for (LocalDate current : ldSet){
		    		createParticipantRow(worksheet, day++, current, p, rowIdx++);
		    	}*/
		    	int epochNumber = 1;
		    	for (ActicalEpoch epoch : ema.getPreviousEpochsInclusive()){
		    		createEmaRow(worksheet, ema, epoch, epochNumber++, rowIdx++);
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
	
	// String[] cols = {"ID", "EMA prompt datetime", "EMA prompt asleep", "Actical Epoch #", 
	// "Actical epoch datetime", "Actical sleep"};

	public Row createEmaRow(Sheet sheet, EMAResult ema, ActicalEpoch epoch, int epochNumber, int rowIdx){
		String emaDateTime = ema.getPrompt().asEpochDateTime();
		int col = 0;
		
		Row row = sheet.createRow(rowIdx);
	    Cell cell1 = row.createCell(col++);
	    cell1.setCellValue(ema.getPrompt().getParticipant());
	    Cell cell2 = row.createCell(col++);
	    cell2.setCellValue(emaDateTime);
	    Cell cell3 = row.createCell(col++);
	    cell3.setCellValue(ema.getPrompt().isAsleep());
	    
	    Cell cell4 = row.createCell(col++);
	    cell4.setCellValue(epochNumber);
	    Cell cell5 = row.createCell(col++);
	    cell5.setCellValue(epoch.asEpochDateTime());
	    Cell cell6 = row.createCell(col++);
	    cell6.setCellValue(epoch.isAsleep());
	    
	    return row;
	}
}
