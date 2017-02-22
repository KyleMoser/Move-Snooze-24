package analysis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class SleepStats {
	LocalDate localDate;
	LocalDateTime sleepOnset;
	LocalDateTime sleepOffset;
	
	/**
	 * Colloquially, the sleep onset is the time a person initially fell asleep on a particular date.
	 * This method finds the sleep onset as follows. Find an epoch coded as asleep between 8PM and 12:00AM.
	 * If none is found, find one between 6PM and 12:00AM. If still none is found, between 6PM and 8:00AM.
	 * (AM times are obviously the following day; PM times are the same day. First the algorithm works backwards,
	 * assuming that the person may have fallen asleep early in the evening; then, the algorithm expands the 
	 * search to include a larger portion of the following day).
	 * 
	 * @param forDate Find a sleep onset for this date
	 * @param sleepPeriods Sleep onset is the first epoch in one of these sleep periods
	 * @return
	 */
	public static SleepPeriod findSleepOnset(LocalDate forDate, List<SleepPeriod> sleepPeriods){
		LocalDateTime ldtStartInterval = forDate.atTime(19, 59, 59);
		LocalDateTime ldtEndInterval = forDate.plusDays(1).atTime(0,0,1);
		SortedSet<SleepPeriod> spSet = new TreeSet<>(new SleepPeriodSorter());
		spSet.addAll(sleepPeriods);
		
		//Check to find the first sleep period that started between the date at 8PM and the next morning at 12:00AM
		for (SleepPeriod sp : spSet){
			if (between(ldtStartInterval, ldtEndInterval, sp.getStart())){
				return sp;
			}
		}
		
		ldtStartInterval = forDate.atTime(17, 59, 59);
		
		//Check to find the first sleep period that started between the date at 6PM and the next morning at 12:00AM
		for (SleepPeriod sp : spSet){
			if (between(ldtStartInterval, ldtEndInterval, sp.getStart())){
				return sp;
			}
		}
		
		ldtEndInterval = forDate.plusDays(1).atTime(8,0,1);
		
		//Check to find the first sleep period that started between the date at 6PM and the next morning at 8AM
		for (SleepPeriod sp : spSet){
			if (between(ldtStartInterval, ldtEndInterval, sp.getStart())){
				return sp;
			}
		}
		
		return null;
	}
	
	public static SleepPeriod findSleepOffset(LocalDate forDate, List<SleepPeriod> sleepPeriods, SleepPeriod onset){
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
			if (between(ldtStartInterval, ldtEndInterval, sp.getEnd())){
				return sp;
			}
		}
		
		reverseIt = spSet.descendingIterator();
		ldtStartInterval = forDate.plusDays(1).atTime(3, 59, 59);
		
		//See if the person woke up the next day from 4AM to 9AM
		while (reverseIt.hasNext()){
			SleepPeriod sp = reverseIt.next();
			if (between(ldtStartInterval, ldtEndInterval, sp.getEnd())){
				return sp;
			}
		}
		
		reverseIt = spSet.descendingIterator();
		ldtEndInterval = forDate.plusDays(1).atTime(10,0,1);
		
		//See if the person woke up the next day from 4AM to 10AM
		while (reverseIt.hasNext()){
			SleepPeriod sp = reverseIt.next();
			if (between(ldtStartInterval, ldtEndInterval, sp.getEnd())){
				return sp;
			}
		}
		
		return null;
	}
	
	public static boolean between(LocalDateTime start, LocalDateTime end, LocalDateTime sp){
		boolean isBefore = start.isBefore(sp);
		boolean isAfter = end.isAfter(sp);
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
