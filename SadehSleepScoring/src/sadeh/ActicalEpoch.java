package sadeh;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * An ActigraphyEpoch represents one data point for a participant in the sleep data study. 
 * An Actigraphy is a device that measures activity level of the wearer. An activity level 
 * at a specific minute of the day is defined as an Epoch. Each participant's data is collected
 * for five days. Therefore, there should be (60 min/hr * 24 hr/day * 5) total ActigraphyEpochs
 * per participant, per assessment point. Data is collected at 3 different times, which are called
 * assessment points. These are the baseline, 6 months afterwards, and 12 months afterwards.
 * 
 * @author kyle_
 *
 */
public class ActicalEpoch {
	//data is collected and analyzed for each patient at the baseline (initial collection), 6 months, and 12 months
	//and different reports may be produced depending on when the data for this epoch was collected.
	protected String assessmentPoint; 
	protected String participant;
	protected int minuteOfDay; //0 through 59 for first hour; 60 through 119 for second hour, etc
	protected String dayOfWeek; //Monday, Tuesday, ...
	protected int activityLevel; //An unbounded, non-negative number representing wearer's activity, 0 means not moving
	protected LocalDate date; //The date of collection without a time component
	protected LocalDateTime dateTime;
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
	
	public String getParticipant() {
		return participant;
	}

	public void setParticipant(String participant) {
		this.participant = participant;
	}

	@Override
	public String toString(){
		String fdt = dateTime.format(formatter);
		return fdt + ", activity: " + activityLevel;
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
	public String getAssessmentPoint() {
		return assessmentPoint;
	}
	public void setAssessmentPoint(String assessmentPoint) {
		this.assessmentPoint = assessmentPoint;
	}
	public int getMinuteOfDay() {
		return minuteOfDay;
	}
	public void setMinuteOfDay(int minuteOfDay) {
		this.minuteOfDay = minuteOfDay;
	}
	public String getDayOfWeek() {
		return dayOfWeek;
	}
	public void setDayOfWeek(String dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}
	public int getActivityLevel() {
		return activityLevel;
	}
	public void setActivityLevel(int activityLevel) {
		this.activityLevel = activityLevel;
	}
}
