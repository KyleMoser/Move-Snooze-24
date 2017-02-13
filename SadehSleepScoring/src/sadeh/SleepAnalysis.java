package sadeh;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import excel.ParticipantDataParseException;

public class SleepAnalysis {
	public static final int WINDOW = 11;
	public static final int WINDOW_BEFORE = 5;
	public static final int WINDOW_AFTER = 5;
	
	public enum ACTIVITY_LEVEL{
		ASLEEP,
		NAPPING,
		SEDENTARY,
		LIGHT,
		MVPA
	}

	public enum SLEEP_PROBABILITY{
		ASLEEP,
		AWAKE
	}
	
	public static boolean isDaytime(LocalDateTime ldt){
		return ldt.getHour() >= 9 && ldt.getHour() <= 17;
	}
	
	public static ACTIVITY_LEVEL getActivityThreshold(ActicalEpoch epoch) 
			throws ParticipantDataParseException{
		boolean asleep = epoch.isAsleep();
		boolean daytime = epoch.isDaytime();
		int level = epoch.getActivityLevel();
		if (asleep){
			if (daytime){
				return ACTIVITY_LEVEL.NAPPING;
			} else{
				return ACTIVITY_LEVEL.ASLEEP;
			}
		}
		
		if (level >= 0 && level <= 40){
			return ACTIVITY_LEVEL.SEDENTARY;
		} else if (level >= 41 && level <= 2200){
			return ACTIVITY_LEVEL.LIGHT;
		} else if (level >= 2201){
			return ACTIVITY_LEVEL.MVPA;
		} else{
			throw new ParticipantDataParseException("Activity level of " + level + " is not within expected range"
					+ " for paticipant " + epoch.getParticipant());
		}
	}
	
	public static SLEEP_PROBABILITY sadeh(List<ActicalEpoch> sortedEpochs, int currentIndex){
		int[] window = new int[WINDOW];
		int middleEpoch = WINDOW / 2;
		
		window[middleEpoch] = sortedEpochs.get(currentIndex).getActivityLevel();
		
		for (int i = 1; i <= WINDOW_BEFORE; i++){
			int epochIndex = currentIndex - i;
			
			//We have a valid data point for this epoch
			if (epochIndex >= 0){
				ActicalEpoch epoch = sortedEpochs.get(epochIndex);
				int activity = epoch.getActivityLevel();
				window[middleEpoch-i] = activity;
			} else{ //This is the beginning of the data set
				window[middleEpoch-i] = 0;
			}
		}
		
		for (int i = 1; i <= WINDOW_AFTER; i++){
			int epochIndex = currentIndex + i;
			
			//We have a valid data point for this epoch
			if (epochIndex <= sortedEpochs.size()-1){
				ActicalEpoch epoch = sortedEpochs.get(epochIndex);
				int activity = epoch.getActivityLevel();
				window[middleEpoch+i] = activity;
			} else{ //This is the end of the data set
				window[middleEpoch+i] = 0;
			}
		}
		
		
		
		double AVG = average(window);
		int NATS = nats(window);
		int[] firstSixEpochs = Arrays.copyOfRange(window, 0, middleEpoch+1);
		double SD = standardDeviation(firstSixEpochs);
		//System.out.println("Standard deviation for " + sortedEpochs.get(currentIndex) + " is " + SD);
		double LG = naturalLog(sortedEpochs.get(currentIndex).getActivityLevel());
		
		double sadeh = (7.601 - (.065 * AVG) - (1.08 * NATS) - (.056 * SD) - (.703 * LG));
		SLEEP_PROBABILITY prob = (sadeh >= 0) ? SLEEP_PROBABILITY.ASLEEP : SLEEP_PROBABILITY.AWAKE;
		
		System.out.println("***********************************************************");
		System.out.println(sortedEpochs.get(currentIndex));
		printWindow("Sadeh 11-minute window: ", window);
		System.out.println("AVG: " + AVG);
		System.out.println("NATS: " + NATS);
		System.out.println("SD: " + SD);
		System.out.println("LG: " + LG);
		System.out.println("Sadeh: " + sadeh);
		System.out.println("Result: " + prob);
		System.out.println("***********************************************************");
		System.out.println(System.lineSeparator());
		
		return prob;
	}
	
	public static double naturalLog(int activityLevel){
		return Math.log1p(activityLevel);		
	}
	
	public static double standardDeviation(int[] window){
		double[] dubs = new double[window.length];
		for (int i = 0; i < window.length; i++) {
		    dubs[i] = window[i];
		}
		
		StandardDeviation sd = new StandardDeviation();
		return sd.evaluate(dubs);
	}
	
	public static void printWindow(String message, int[] window){
		System.out.println(Arrays.toString(window));
	}
	
	public static int nats(int[] window){
		int total = 0;
		
		for (int i = 0; i < window.length; i++){
			if (window[i] >= 50 && window[i] < 100)
				total++;
		}
		
		return total;
	}
	
	public static double average(int[] window){
		int total = 0;
		
		for (int i = 0; i < window.length; i++){
			total += window[i];
		}
		
		return ((double) total)/((double)window.length);
	}
}
