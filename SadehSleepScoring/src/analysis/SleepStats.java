package analysis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sadeh.ActicalEpoch;
import sadeh.SleepAnalysis.ACTIVITY_LEVEL;

public class SleepStats {
	LocalDate localDate;
	LocalDateTime sleepOnset;
	LocalDateTime sleepOffset;
	long nightSleepPeriod = -1;
	long totalSleepTime = -1;
	long totalWakeTime = -1;
	double sleepEfficiency = -1;
	double percentDailySleep = -1;
	long eightToEight = -1;
	int sedentary = -1;
	int light = -1;
	int mvpa = -1;
	
	
	public SleepStats(LocalDate localDate, SleepPeriod onset, SleepPeriod offset){
		this.localDate = localDate;
		
		if (onset != null)
			sleepOnset = onset.getStart();
		
		if (offset != null)
			sleepOffset = offset.getEnd();
	}
	
	public long getNightSleepPeriod() {
		return nightSleepPeriod;
	}

	public void setNightSleepPeriod(long nightSleepPeriod) {
		this.nightSleepPeriod = nightSleepPeriod;
	}

	public long getTotalSleepTime() {
		return totalSleepTime;
	}

	public void setTotalSleepTime(long totalSleepTime) {
		this.totalSleepTime = totalSleepTime;
	}

	public long getTotalWakeTime() {
		return totalWakeTime;
	}

	public void setTotalWakeTime(long totalWakeTime) {
		this.totalWakeTime = totalWakeTime;
	}
	
	public double getSleepEfficiency() {
		return sleepEfficiency;
	}

	public void setSleepEfficiency(double sleepEfficiency) {
		this.sleepEfficiency = sleepEfficiency;
	}

	public double getPercentDailySleep() {
		return percentDailySleep;
	}

	public void setPercentDailySleep(double percentDailySleep) {
		this.percentDailySleep = percentDailySleep;
	}

	public long getEightToEight() {
		return eightToEight;
	}

	public void setEightToEight(long eightToEight) {
		this.eightToEight = eightToEight;
	}

	public int getSedentary() {
		return sedentary;
	}

	public void setSedentary(int sedentary) {
		this.sedentary = sedentary;
	}

	public int getLight() {
		return light;
	}

	public void setLight(int light) {
		this.light = light;
	}

	public int getMvpa() {
		return mvpa;
	}

	public void setMvpa(int mvpa) {
		this.mvpa = mvpa;
	}

	public long calculateNightSleepPeriod(){
		if (sleepOnset == null || sleepOffset == null)
			return -1;
		
		return Math.abs(ChronoUnit.MINUTES.between(sleepOnset, sleepOffset) + 1);
	}
	
	public long calculateTotalSleepTime(List<ActicalEpoch> epochs){
		Stream<ActicalEpoch> eStream = 
				epochs.stream().filter(epoch -> epoch.isAsleep() && between(sleepOnset, sleepOffset, epoch.getDateTime()));
		List<ActicalEpoch> results = eStream.collect(Collectors.toList());
		return results.size();
	}
	
	public long calculateTotalTimeBasedNightSleep(List<ActicalEpoch> epochs){
		LocalDateTime ldtStartInterval = localDate.atTime(20, 0, 0);
		LocalDateTime ldtEndInterval = localDate.plusDays(1).atTime(8,0,0);
		
		Stream<ActicalEpoch> eStream = 
				epochs.stream().filter(epoch -> epoch.isAsleep() && between(ldtStartInterval, ldtEndInterval, epoch.getDateTime()));
		List<ActicalEpoch> results = eStream.collect(Collectors.toList());
		return results.size();
	}
	
	public int calculateByActivityLevel(List<ActicalEpoch> epochs, ACTIVITY_LEVEL lvl){
		Stream<ActicalEpoch> eStream = 
				epochs.stream().filter(epoch -> epoch.getDate().isEqual(localDate) 
						&& epoch.getActivityThreshold() == lvl);
		List<ActicalEpoch> results = eStream.collect(Collectors.toList());
		return results.size();
	}
	
	public double calculatePercentDailySleep(List<ActicalEpoch> epochs){
		Stream<ActicalEpoch> eStream = 
				epochs.stream().filter(epoch -> epoch.getDate().isEqual(localDate) && epoch.isAsleep());
		List<ActicalEpoch> results = eStream.collect(Collectors.toList());
		
		
		Stream<ActicalEpoch> allEpochsForDate = 
				epochs.stream().filter(epoch -> epoch.getDate().isEqual(localDate));
		List<ActicalEpoch> allEpsForDate = allEpochsForDate.collect(Collectors.toList());
		
		return ((double)results.size()/(double)allEpsForDate.size());
	}
	
	public long calculateTotalWakeTime(long totalSleepTime){
		return Math.abs(calculateNightSleepPeriod() - totalSleepTime);
	}
	
	public double calculateSleepEfficiency(long totalSleepTime, long nightSleepPeriod){
		return ((double) totalSleepTime/(double)nightSleepPeriod);
	}
	
	/**
	 * Colloquially, the sleep onset is the time a person initially fell asleep on a particular date.
	 * This method finds the sleep onset as follows. Find an epoch coded as asleep between 7:30PM and 11:30PM.
	 * This epoch must be the first epoch of a sleep period; otherwise, it is ineligible to be selected.
	 * If none is found, find one between 5:30PM and 11:30PM. If still none is found, between 5:30PM and 8:00AM.
	 * (AM times are obviously the following day; PM times are the same day. First the algorithm works backwards,
	 * assuming that the person may have fallen asleep early in the evening; then, the algorithm expands the 
	 * search to include a larger portion of the following day).
	 * 
	 * @param forDate Find a sleep onset for this date
	 * @param sleepPeriods Sleep onset is the first epoch in one of these sleep periods
	 * @return
	 */
	public static SleepPeriod findSleepOnset(LocalDate forDate, List<SleepPeriod> sleepPeriods){
		LocalDateTime ldtStartInterval = forDate.atTime(19, 29, 0);
		LocalDateTime ldtEndInterval = forDate.atTime(23,30,1);
		SortedSet<SleepPeriod> spSet = new TreeSet<>(new SleepPeriodSorter());
		spSet.addAll(sleepPeriods);
		
		//Check to find the first sleep period that started between the date at 7:30PM-11:30PM
		for (SleepPeriod sp : spSet){
			if (between(ldtStartInterval, ldtEndInterval, sp.getStart())){
				return sp;
			}
		}
		
		ldtStartInterval = forDate.atTime(17, 29, 0);
		
		//Check to find the first sleep period that started between the date at 5:30PM-11:30PM
		for (SleepPeriod sp : spSet){
			if (between(ldtStartInterval, ldtEndInterval, sp.getStart())){
				return sp;
			}
		}
		
		ldtEndInterval = forDate.plusDays(1).atTime(8,0,1);
		
		//Check to find the first sleep period that started between the date at 5:30PM and the next morning at 8AM
		for (SleepPeriod sp : spSet){
			if (between(ldtStartInterval, ldtEndInterval, sp.getStart())){
				return sp;
			}
		}
		
		return null;
	}
	
	public static SleepPeriod findSleepOffset(LocalDate forDate, List<SleepPeriod> sleepPeriods, SleepPeriod onset){
		if (onset == null)
			return null;
		
		LocalDateTime ldtStartInterval = forDate.plusDays(1).atTime(5, 59, 59);
		LocalDateTime ldtEndInterval = forDate.plusDays(1).atTime(9,0,1);
		TreeSet<SleepPeriod> spSet = new TreeSet<>(new SleepPeriodSorter());
		spSet.addAll(sleepPeriods);
		
		//A person cannot fall asleep after they wake up so we filter impossible times from the search set
		Iterator<SleepPeriod> matchIt = spSet.descendingIterator();
		boolean foundOnset = false;
		while (matchIt.hasNext()){
			SleepPeriod sp = matchIt.next();
			
			if (foundOnset)
				matchIt.remove();
			
			//The person fell asleep during this interval
			if (sp.equals(onset))
				foundOnset = true;
		}
		
		Iterator<SleepPeriod> reverseIt = spSet.descendingIterator();
		
		//See if the person woke up the next day from 6AM to 9AM
		while (reverseIt.hasNext()){
			SleepPeriod sp = reverseIt.next();
			if (between(ldtStartInterval, ldtEndInterval, sp.getStart())){ //these used to be sp.getEnd()
				return sp;
			}
		}
		
		reverseIt = spSet.descendingIterator();
		ldtStartInterval = forDate.plusDays(1).atTime(3, 59, 59);
		
		//See if the person woke up the next day from 4AM to 9AM
		while (reverseIt.hasNext()){
			SleepPeriod sp = reverseIt.next();
			if (between(ldtStartInterval, ldtEndInterval, sp.getStart())){
				return sp;
			}
		}
		
		reverseIt = spSet.descendingIterator();
		ldtEndInterval = forDate.plusDays(1).atTime(10,0,1);
		
		//See if the person woke up the next day from 4AM to 10AM
		while (reverseIt.hasNext()){
			SleepPeriod sp = reverseIt.next();
			if (between(ldtStartInterval, ldtEndInterval, sp.getStart())){
				return sp;
			}
		}
		
		return null;
	}
	
	public static boolean between(LocalDateTime start, LocalDateTime end, LocalDateTime sp){
		boolean isBefore = start.isBefore(sp) || start.isEqual(sp);
		boolean isAfter = end.isAfter(sp) || end.isEqual(sp);
		return isBefore && isAfter;
	}
	
	public static class SleepPeriodSorter implements Comparator<SleepPeriod>{
		@Override
		public int compare(SleepPeriod sp1, SleepPeriod sp2) {
			return sp1.getStart().compareTo(sp2.getStart());
		}
	}
	
	public LocalDate getLocalDate() {
		return localDate;
	}
	public void setLocalDate(LocalDate localDate) {
		this.localDate = localDate;
	}
	public LocalDateTime getSleepOnset() {
		return sleepOnset;
	}
	public void setSleepOnset(LocalDateTime sleepOnset) {
		this.sleepOnset = sleepOnset;
	}
	public LocalDateTime getSleepOffset() {
		return sleepOffset;
	}
	public void setSleepOffset(LocalDateTime sleepOffset) {
		this.sleepOffset = sleepOffset;
	}
}
