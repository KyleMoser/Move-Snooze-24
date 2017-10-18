package ema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An EMAPrompt represents an SMS message sent to the mother of a child whose sleep habits are being collected.
 * The mother does not have to respond to the SMS, so the Excel document that these data points are collected from
 * may contain incomplete data. An EMAPrompt represents one point of data corresponding to a particular minute of
 * a particular day, which is usually parsed from an Excel document.
 * 
 * @author kyle_
 *
 */
public class EMAPrompt {
	protected boolean asleep;
	protected boolean responsed = false;
	protected String participant;
	protected LocalDate date; //The date of collection without a time component
	protected LocalDateTime dateTime;
	DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH:mm");
	public DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
	static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
	
	
	public boolean isResponsed() {
		return responsed;
	}

	public void setResponsed(boolean responsed) {
		this.responsed = responsed;
	}

	public String asEpochTime(){
		LocalDateTime ldt = getDateTime();
    	String hourMinute = ldt.format(hourFormatter);
    	return hourMinute;
	}
	
	public String asEpochDateTime(){
		LocalDateTime ldt = getDateTime();
    	String fdt = ldt.format(formatter);
    	return fdt;
	}
	
	public static String asEpochDateTime(LocalDateTime ldt){
    	String fdt = ldt.format(formatter);
    	return fdt;
	}
	
	/**
	 * @return true if the individual is asleep false otherwise
	 */
	public boolean isAsleep() {
		return asleep;
	}

	public void setAsleep(boolean asleep) {
		this.asleep = asleep;
	}

	public String getParticipant() {
		return participant;
	}

	public void setParticipant(String participant) {
		this.participant = participant;
	}

	@Override
	public String toString(){
		String fdt = dateTime.format(formatter);
		if (responsed){
			return "Participant " + getParticipant() + ", date: " + fdt + ", asleep: " + isAsleep();
		} else{
			return "Participant " + getParticipant() + ", date: " + fdt + ", did not respond.";
		}
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}
	public void setDateTime(LocalDateTime dateTime) {
		this.dateTime = dateTime;
	}
	public LocalDate getDate() {
		return date;
	}
	public void setDate(LocalDate date) {
		this.date = date;
	}
}
