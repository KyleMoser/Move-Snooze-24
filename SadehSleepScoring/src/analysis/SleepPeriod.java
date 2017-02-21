package analysis;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import sadeh.ActicalEpoch;

public class SleepPeriod {
	LocalDateTime start;
	LocalDateTime end;
	SortedSet<ActicalEpoch> epochs = null;
	
	public LocalDateTime getStart() {
		return start;
	}

	public void setStart(LocalDateTime start) {
		this.start = start;
	}

	public LocalDateTime getEnd() {
		return end;
	}

	public void setEnd(LocalDateTime end) {
		this.end = end;
	}

	public SleepPeriod(List<ActicalEpoch> sleepochs){
		epochs = new TreeSet<>(new EpochSorter());
		epochs.addAll(sleepochs);
		this.start = epochs.first().getDateTime();
		this.end = epochs.last().getDateTime();
	}
	
	class EpochSorter implements Comparator<ActicalEpoch>{
		@Override
		public int compare(ActicalEpoch epoch1, ActicalEpoch epoch2) {
			return epoch1.getDateTime().compareTo(epoch2.getDateTime());
		}
	}
}
