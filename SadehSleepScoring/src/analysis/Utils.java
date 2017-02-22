package analysis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {
	static DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH:mm");
	static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
	static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
	
	public String asMilitaryTime(LocalDateTime ldt){
    	String hourMinute = ldt.format(hourFormatter);
    	return hourMinute;
	}
	
	public static String asDate(LocalDateTime ldt){
    	String fdt = ldt.format(dateFormatter);
    	return fdt;
	}
	
	public static String asDateTime(LocalDateTime ldt){
    	String fdt = ldt.format(dateTimeFormatter);
    	return fdt;
	}
	
	public String asMilitaryTime(LocalDate ldt){
    	String hourMinute = ldt.format(hourFormatter);
    	return hourMinute;
	}
	
	public static String asDate(LocalDate ldt){
    	String fdt = ldt.format(dateFormatter);
    	return fdt;
	}
	
	public static String asDateTime(LocalDate ldt){
    	String fdt = ldt.format(dateTimeFormatter);
    	return fdt;
	}
}
