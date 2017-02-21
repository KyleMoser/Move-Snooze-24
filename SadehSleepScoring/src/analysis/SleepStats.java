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
	
	public static SleepPeriod findSleepOnset(LocalDate forDate, List<SleepPeriod> sleepPeriods){
		LocalDateTime ldtStartInterval = forDate.atTime(19, 59, 59);
		LocalDateTime ldtEndInterval = forDate.plusDays(1).atTime(0,0,1);
		SortedSet<SleepPeriod> spSet = new TreeSet<>(new SleepPeriodSorter());
		spSet.addAll(sleepPeriods);
		
		for (SleepPeriod sp : spSet){
			if (between(ldtStartInterval, ldtEndInterval, sp)){
				return sp;
			}
		}
		
		ldtStartInterval = forDate.atTime(17, 59, 59);
		
		for (SleepPeriod sp : spSet){
			if (between(ldtStartInterval, ldtEndInterval, sp)){
				return sp;
			}
		}
		
		ldtEndInterval = forDate.plusDays(1).atTime(8,0,1);
		
		for (SleepPeriod sp : spSet){
			if (between(ldtStartInterval, ldtEndInterval, sp)){
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
		
		//The sleep onset and sleep offset times can potentially overlap; and obviously, a person
		//cannot fall asleep after (or at the same time) they wake up. Therefore, sleep periods before
		//the sleep onset are removed, so the sleep offset will definitely be after the onset.
		Iterator<SleepPeriod> matchIt = spSet.descendingIterator();
		boolean foundOnset = false;
		while (matchIt.hasNext()){
			SleepPeriod sp = matchIt.next();
			if (sp.equals(onset))
				foundOnset = true;
			
			if (foundOnset)
				matchIt.remove();
		}
		
		Iterator<SleepPeriod> reverseIt = spSet.descendingIterator();
		
		while (reverseIt.hasNext()){
			SleepPeriod sp = reverseIt.next();
			if (between(ldtStartInterval, ldtEndInterval, sp)){
				return sp;
			}
		}
		
		reverseIt = spSet.descendingIterator();
		ldtStartInterval = forDate.plusDays(1).atTime(3, 59, 59);
		
		while (reverseIt.hasNext()){
			SleepPeriod sp = reverseIt.next();
			if (between(ldtStartInterval, ldtEndInterval, sp)){
				return sp;
			}
		}
		
		reverseIt = spSet.descendingIterator();
		ldtEndInterval = forDate.plusDays(1).atTime(9,0,1);
		
		while (reverseIt.hasNext()){
			SleepPeriod sp = reverseIt.next();
			if (between(ldtStartInterval, ldtEndInterval, sp)){
				return sp;
			}
		}
		
		return null;
	}
	
	public static boolean between(LocalDateTime start, LocalDateTime end, SleepPeriod sp){
		LocalDateTime sleepPeriodStart = sp.getStart();
		boolean isBefore = start.isBefore(sleepPeriodStart);
		boolean isAfter = end.isAfter(sleepPeriodStart);
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
