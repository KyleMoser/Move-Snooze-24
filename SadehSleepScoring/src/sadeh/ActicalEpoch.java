package sadeh;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

import sadeh.SleepAnalysis.ACTIVITY_LEVEL;
import sadeh.SleepAnalysis.SLEEP_PROBABILITY;

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
	protected boolean daytime;
	protected boolean asleep;
	protected String participant;
	protected int minuteOfDay; //0 through 59 for first hour; 60 through 119 for second hour, etc
	protected String dayOfWeek; //Monday, Tuesday, ...
	protected int activityLevel; //An unbounded, non-negative number representing wearer's activity, 0 means not moving
	protected LocalDate date; //The date of collection without a time component
	protected LocalDateTime dateTime;
	protected ACTIVITY_LEVEL activityThreshold;
	protected SLEEP_PROBABILITY sleepState;
	
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
	
	public SLEEP_PROBABILITY getSleepState() {
		return sleepState;
	}

	public void setSleepState(SLEEP_PROBABILITY sleepState) {
		this.sleepState = sleepState;
	}

	public boolean isDaytime() {
		return daytime;
	}

	public void setDaytime(boolean daytime) {
		this.daytime = daytime;
	}

	public boolean isAsleep() {
		return asleep;
	}

	public void setAsleep(boolean asleep) {
		this.asleep = asleep;
	}

	public ACTIVITY_LEVEL getActivityThreshold() {
		return activityThreshold;
	}

	public void setActivityThreshold(ACTIVITY_LEVEL activtyThreshold) {
		this.activityThreshold = activtyThreshold;
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
