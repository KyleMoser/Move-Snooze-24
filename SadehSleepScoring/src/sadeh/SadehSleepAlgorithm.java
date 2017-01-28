package sadeh;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

public class SadehSleepAlgorithm {
	public static final int WINDOW = 11;
	public static final int WINDOW_BEFORE = 5;
	public static final int WINDOW_AFTER = 5;

	enum SLEEP_PROBABILITY{
		ASLEEP,
		AWAKE
	}
	
	public static SLEEP_PROBABILITY sadeh(List<ActicalEpoch> sortedEpochs, int currentIndex){
		int[] window = new int[WINDOW];
		int middleEpoch = WINDOW / 2;
		
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
		
		//printWindow("Standard deviation input: ", window);
		StandardDeviation sd = new StandardDeviation();
		sd.setBiasCorrected(false);
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
