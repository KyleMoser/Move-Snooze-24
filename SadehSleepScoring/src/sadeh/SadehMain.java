package sadeh;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import excel.ActicalDataOutputException;
import excel.ActicalExcelParser;
import excel.ActivityThresholdWorkbook;
import excel.ParticipantDataParseException;
import excel.ParticipantWorkbook;

import java.util.Set;
import java.util.stream.Stream;
import analysis.SleepPeriod;
import analysis.SleepStats;
import analysis.Utils;
import sadeh.SleepAnalysis.ACTIVITY_LEVEL;
import sadeh.SleepAnalysis.SLEEP_PROBABILITY;

/**
 * Processes excel documents containing Actical data. An actical is a wrist or ankle worn
 * device that measures movement (activity level). This data is recorded for a time period determined by the
 * researcher and wearer, and is coded into excel documents (one document per participant). In the document,
 * each row of data contains the time (minute) the data was recorded and the activity level at that time as well
 * as the date (e.g. day of the week) that the data was recorded.
 *
 * @author kyle_
 *
 */
public class SadehMain {
	
	public static void main(String[] args){
		String inputPath = args[0];
		String outputPath = args[1];
		String assessmentPoint = args[2];
		
		try {
			System.setOut(new PrintStream(new File(outputPath + "\\results.txt")));
			List<ActicalParticipant> participants = new ArrayList<>();
			
			try(Stream<Path> paths = Files.walk(Paths.get(inputPath), 1)) {
			    paths.forEach(filePath -> {
			        if (Files.isRegularFile(filePath)) {
			            System.out.println(filePath);
			            ActicalParticipant p = process(filePath.toFile(), assessmentPoint, outputPath);
			            participants.add(p);
			        }
			    });
			} catch (Exception ex){
				ex.printStackTrace();
				System.out.println(ex.getMessage());
			}
			
			ParticipantWorkbook pwb = new ParticipantWorkbook(participants);
			pwb.create();
			pwb.write(outputPath + "\\participantData.xlsx");
		} catch (Exception e){
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
	
	private static String getParticipantName(File file){
		String[] names = file.getName().split("\\.");
		return names[0];
	}
	
	/**
	 * A sleep period uses the data from the sadeh algorithm to find time periods where an 
	 * individual is sleeping. 15 or more epochs of asleep (or napping) preceded and followed by 5 minutes of 
	 * awake (e.g. light, sedentary, or mvpa activity level) counts as a sleep period. Once 15 consecutive epochs
	 * of sleep are reached, all other epochs still count as part of the sleep period up until there are 5 consecutive 
	 * awake epochs. For example, if the 16th epoch is awake but the 17th is asleep, then 18-22 are all awake, the sleep
	 * period would contain epochs 1-17, even though epoch 16 was awake.
	 * 
	 * @return
	 * @throws ParticipantDataParseException 
	 */
	public static List<SleepPeriod> getSleepPeriods(List<ActicalEpoch> epochs) throws ParticipantDataParseException{
		List<SleepPeriod> sleepPeriods = new ArrayList<>();
		List<ActicalEpoch> possibleSleepPeriod = new ArrayList<>();
		List<PossibleSleepPeriod> psps = new ArrayList<>();
		int sleepStartingIndex = -1;
		
		/*
		 * Find any groups of 15 consecutive epochs that are coded as "asleep" or "napping". 
		 * This is a possible sleep period. To determine if it is actually a sleep period,
		 * we will later calculate whether the group is preceded (and followed) by 5 or more
		 * consecutive "awake" epochs.
		 */
		for (int i = 0; i < epochs.size(); i++){
			ActicalEpoch current = epochs.get(i);
			if (current.isAsleep()){
				if (possibleSleepPeriod.isEmpty()){
					sleepStartingIndex = i;
				}
				
				possibleSleepPeriod.add(current);
			} 
			
			if (!current.isAsleep() || i == epochs.size()-1){
				if (possibleSleepPeriod.size() >= 15){
					PossibleSleepPeriod psp = new PossibleSleepPeriod();
					psp.epochListStartingIndex = sleepStartingIndex;
					psp.epochs = possibleSleepPeriod;
					psps.add(psp);
				}
				
				possibleSleepPeriod = new ArrayList<>();
				sleepStartingIndex = -1;
			}
		}
		
		/*
		 * For each possible sleep period, find the end of the sleep period by finding the point where
		 * there are 5 consecutive "awake" epochs. If there is no such point (which might happen at the end
		 * of the data set) the possible sleep period will be discarded, since it isn't really a sleep period.
		 */
		Iterator<PossibleSleepPeriod> pspIt = psps.iterator();
		while (pspIt.hasNext()){
			PossibleSleepPeriod psp = pspIt.next();
			List<ActicalEpoch> eps = psp.epochs;
			int possibleSleepEpochDsIdx = epochs.indexOf(eps.get(eps.size()-1));
			//Verify that the possible sleep period's starting index is the same as the index of the epoch in the data set 
			if (possibleSleepEpochDsIdx != psp.epochListStartingIndex+eps.size()-1)
				throw new ParticipantDataParseException("Possible sleep period starting at " + eps.get(0).asEpochDateTime() 
						+ " starting index of " + (psp.epochListStartingIndex+eps.size()) + " does not match expected index of " + possibleSleepEpochDsIdx);
			
			int consecutiveAwake = 0;
			ArrayList<ActicalEpoch> additionalSleepochs = new ArrayList<>();
			ArrayList<ActicalEpoch> tempSleepochs = new ArrayList<>();
			boolean hasWakePeriod = false; //is there data indicating person woke up; if not it's probably the end of the data collected
			
			//work forwards to see if there are 5 consecutive awake epochs after this sleep period.
			//also, the possible sleep period only contains 15 epochs (the minimum) but might actually
			//have more; this loop will add any additional epochs to the sleep period as necessary.
			//(See the definition of 'sleep period' in the method Javadoc for more information)
			for (int i = possibleSleepEpochDsIdx+1; i < epochs.size(); i++){
				ActicalEpoch epoch = epochs.get(i);
				if (!epoch.isAsleep()){
					consecutiveAwake++;
					//The current epoch might be part of the sleep period or might indicate the end of the sleep period.
					//That determination depends on whether it is part of 5 consecutive 'awake' epochs or not.
					tempSleepochs.add(epoch);
				} else{
					consecutiveAwake = 0;
					additionalSleepochs.addAll(tempSleepochs);
					additionalSleepochs.add(epoch);
					tempSleepochs = new ArrayList<>();
				}
				
				if (consecutiveAwake >= 5){
					psp.epochs.addAll(additionalSleepochs);
					hasWakePeriod = true;
					break;
				}
			}
			
			if (!hasWakePeriod){
				System.out.println("Possible sleep period of 15+ sleep epochs starting at " + eps.get(0).asEpochDateTime()
						+ " is determined not to be a sleep period because it does not end with 5 awake epochs.");
				pspIt.remove();
			}
		}
		
		for (PossibleSleepPeriod psp : psps){
			List<ActicalEpoch> eps = psp.epochs;
			System.out.println("Possible sleep period: individual has 15+ sleep epochs starting at " + eps.get(0).asEpochDateTime());

			//for the first epoch in the sleep period, find its place within the data set
			int possibleSleepEpochDsIdx = epochs.indexOf(eps.get(0));
			//Verify that the possible sleep period's starting index is the same as the index of the first epoch in the data set (it should be)
			if (possibleSleepEpochDsIdx != psp.epochListStartingIndex)
				throw new ParticipantDataParseException("Possible sleep period starting at " + eps.get(0).asEpochDateTime() 
						+ " starting index of " + psp.epochListStartingIndex + " does not match expected index of " + possibleSleepEpochDsIdx);
			
			int consecutiveAwake = 0;
			//work backwards to see if there are 5 consecutive awake epochs before this sleep period
			for (int i = psp.epochListStartingIndex-1; i >= 0; i--){
				ActicalEpoch epoch = epochs.get(i);
				if (!epoch.isAsleep()){
					consecutiveAwake++;
				} else{
					break;
				}
				
				if (consecutiveAwake >= 5){
					SleepPeriod sp = new SleepPeriod(psp.epochs);
					sleepPeriods.add(sp);
					break;
				}
			}
		}
		
		return sleepPeriods;
	}
	
	public static NapData calculateNapData(List<ActicalEpoch> epochs){
		int consecutiveNappingEpochs = 0;
		List<Integer> naps = new ArrayList<>();
		double averageNapDurationMinutes = 0;
		int minNapDurationMinutes = Integer.MAX_VALUE;
		int maxNapDurationMinutes = 0;
		
		for (int i = 0; i < epochs.size(); i++){
			ActicalEpoch epoch = epochs.get(i);
			boolean napping = epoch.getActivityThreshold() == ACTIVITY_LEVEL.NAPPING;
			
			if (napping){
				consecutiveNappingEpochs++;
			} 
			
			if ((!napping && consecutiveNappingEpochs >= 30) ||
					(i == epochs.size()-1 && consecutiveNappingEpochs >= 30)){
				naps.add(consecutiveNappingEpochs);
				consecutiveNappingEpochs = 0;
			}
			
		}
		
		int totalNapTime = 0;
		
		for (Integer nap : naps){
			totalNapTime += nap;
			if (nap < minNapDurationMinutes)
				minNapDurationMinutes = nap;
			
			if (nap > maxNapDurationMinutes)
				maxNapDurationMinutes = nap;
		}
		
		if (minNapDurationMinutes == Integer.MAX_VALUE)
			minNapDurationMinutes = 0;
		
		if (naps.size() > 0)
			averageNapDurationMinutes = totalNapTime/naps.size();
		
		long avgNap = Math.round(averageNapDurationMinutes);
		NapData napData = new NapData();
		napData.setAverageNap(avgNap);
		napData.setMaxNap(maxNapDurationMinutes);
		napData.setMinNap(minNapDurationMinutes);
		napData.setNumberNaps(naps.size());
		return napData;
	}
	
	public static ActicalParticipant process(File excel, String assessmentPoint, String outputPath){
		try {
			ActicalParticipant participant = new ActicalParticipant();
			participant.setAssessmentPoint(assessmentPoint);
			String name = getParticipantName(excel);
			participant.setParticipant(name);
			List<ActicalEpoch> epochs = parseParticipantData(excel);
			participant.setSleepData(epochs);
			
			for (ActicalEpoch epoch : epochs){
				participant.addEpochToDateBasedMap(epoch);
			}
			
			HashMap<String, List<ActicalEpoch>> map = participant.getDateEpochMap();
			Set<Entry<String, List<ActicalEpoch>>> entries = map.entrySet();
			for (Entry<String, List<ActicalEpoch>> entry : entries){
				List<ActicalEpoch> currentEpochs = entry.getValue();
				NapData napData = calculateNapData(currentEpochs);
				participant.getNapMap().put(entry.getKey(), napData);
			}
			
			List<SleepPeriod> sleepPeriods = getSleepPeriods(epochs);
			for (SleepPeriod sp : sleepPeriods){
				System.out.println("Found sleep period starting at " + ActicalEpoch.asEpochDateTime(sp.getStart())
					+ " and ending at " + ActicalEpoch.asEpochDateTime(sp.getEnd()));
			}
			
			Set<String> dates = participant.getDateEpochMap().keySet();
			List<LocalDate> dataCollectionDates = new ArrayList<>();
			for (String date : dates){
				LocalDate ld = LocalDate.parse(date, ActicalParticipant.formatter);
				dataCollectionDates.add(ld);
			}
			
			Collections.sort(dataCollectionDates);
			System.out.println(System.lineSeparator());
			
			for (LocalDate date : dataCollectionDates){
				SleepPeriod sleepOnset = SleepStats.findSleepOnset(date, sleepPeriods);
				SleepPeriod sleepOffset = SleepStats.findSleepOffset(date, sleepPeriods, sleepOnset);
				
				if (sleepOnset != null && sleepOffset != null){
					System.out.println("Participant: " + participant.getParticipant() + ", date: " + Utils.asDate(date)
						+ ", sleep onset: " + Utils.asDateTime(sleepOnset.getStart())
						+ ", sleep offset: " + Utils.asDateTime(sleepOffset.getEnd()));
				} else if (sleepOnset == null){
					System.out.println("Participant: " + participant.getParticipant() + ", date: " + Utils.asDate(date)
					+ ", sleep onset was not found.");
				} else if (sleepOffset == null){
					System.out.println("Participant: " + participant.getParticipant() + ", date: " + Utils.asDate(date)
					+ ", sleep offset was not found.");
				}
				
				SleepStats sleep = new SleepStats(date, sleepOnset, sleepOffset);
				if (sleepOnset != null && sleepOffset != null){
					long nightSleepPeriod = sleep.calculateNightSleepPeriod();
					sleep.setNightSleepPeriod(nightSleepPeriod);
					long totalSleepTime = sleep.calculateTotalSleepTime(epochs);
					sleep.setTotalSleepTime(totalSleepTime);
					long totalWakeTime = sleep.calculateTotalWakeTime(totalSleepTime);
					sleep.setTotalWakeTime(totalWakeTime);
					double sleepEfficiency = sleep.calculateSleepEfficiency(totalSleepTime, nightSleepPeriod);
					sleep.setSleepEfficiency(sleepEfficiency);
					System.out.println("Participant: " + participant.getParticipant() + ", date: " + Utils.asDate(date)
					+ ", night sleep period: " + nightSleepPeriod);
					System.out.println("Participant: " + participant.getParticipant() + ", date: " + Utils.asDate(date)
					+ ", total sleep time: " + totalSleepTime);
					System.out.println("Participant: " + participant.getParticipant() + ", date: " + Utils.asDate(date)
					+ ", total wake time: " + totalWakeTime);
					System.out.println("Participant: " + participant.getParticipant() + ", date: " + Utils.asDate(date)
					+ ", sleep efficiency: " + sleepEfficiency);
				}
				
				//you can do these with or without sleep onset/offset
				double percentDailySleep = sleep.calculatePercentDailySleep(epochs);
				sleep.setPercentDailySleep(percentDailySleep);
				long eightToEight = sleep.calculateTotalTimeBasedNightSleep(epochs);
				sleep.setEightToEight(eightToEight);
				int sedentary = sleep.calculateByActivityLevel(epochs, ACTIVITY_LEVEL.SEDENTARY);
				sleep.setSedentary(sedentary);
				int light = sleep.calculateByActivityLevel(epochs, ACTIVITY_LEVEL.LIGHT);
				sleep.setLight(light);
				int mvpa = sleep.calculateByActivityLevel(epochs, ACTIVITY_LEVEL.MVPA);
				sleep.setMvpa(mvpa);
				
				
				System.out.println("Participant: " + participant.getParticipant() + ", date: " + Utils.asDate(date)
				+ ", percent daily sleep: " + percentDailySleep);
				System.out.println("Participant: " + participant.getParticipant() + ", date: " + Utils.asDate(date)
				+ ", total time based night sleep: " + eightToEight);
				System.out.println("Participant: " + participant.getParticipant() + ", date: " + Utils.asDate(date)
				+ ", sedentary: " + sedentary);
				System.out.println("Participant: " + participant.getParticipant() + ", date: " + Utils.asDate(date)
				+ ", light: " + light);
				System.out.println("Participant: " + participant.getParticipant() + ", date: " + Utils.asDate(date)
				+ ", mvpa: " + mvpa);
				
				participant.addSleepStatsToDateBasedMap(sleep);
			}
			
			
			ActivityThresholdWorkbook atw = new ActivityThresholdWorkbook(epochs);
			atw.create();
			atw.write(outputPath + "\\" + name + "_" + assessmentPoint + ".xlsx");
			
			return participant;
		} catch (ParticipantDataParseException e) {
			System.out.println(e.getMessage());
		} catch (ActicalDataOutputException e) {
			System.out.println(e.getMessage());
		}
		
		return null;
	}
	
	public static List<ActicalEpoch> parseParticipantData(File excel) throws ParticipantDataParseException{
		List<ActicalEpoch> participantEpochs = ActicalExcelParser.parseSadehExcelDocument(excel);
		
		//Sort the Actical data by date, earlier dates first
		Collections.sort(participantEpochs, (a, b) -> a.getDateTime().compareTo(b.getDateTime()));
		participantEpochs.forEach(epoch -> System.out.println(epoch));
		
		for (int i = 0; i < participantEpochs.size(); i++){
			if (!validateEpoch(participantEpochs, i)){
				ActicalEpoch current = participantEpochs.get(i);
				ActicalEpoch previous = participantEpochs.get(i-1);
				
				throw new ParticipantDataParseException("File format is invalid for the file " 
						+ excel.getAbsolutePath() + ", it must be manually processed."
						+ " Cause: there is more than one minute difference between the epoch with date "
						+ previous.getDateTime() + " and the next consecutive epoch with date " 
						+ current.getDateTime());
			}
			
			SLEEP_PROBABILITY sleepState = SleepAnalysis.sadeh(participantEpochs, i);
			ActicalEpoch epoch = participantEpochs.get(i);
			epoch.setSleepState(sleepState);
			boolean isAsleep = (sleepState == SLEEP_PROBABILITY.ASLEEP);
			boolean isDaytime = SleepAnalysis.isDaytime(epoch.getDateTime());
			epoch.setAsleep(isAsleep);
			epoch.setDaytime(isDaytime);
			ACTIVITY_LEVEL lvl = SleepAnalysis.getActivityThreshold(epoch);
			epoch.setActivityThreshold(lvl);
		}
		
		return participantEpochs;
	}
	
	/*
	 * Every epoch should be within one minute from the previous epoch
	 */
	private static boolean validateEpoch(List<ActicalEpoch> epochs, int index) {
		if (epochs == null)
			return false;

		if (index == 0 || epochs.size() < 2)
			return true;

		ActicalEpoch currentEpoch = epochs.get(index);
		ActicalEpoch previousEpoch = epochs.get(index-1);
		long minutes = 
				Math.abs(Duration.between(currentEpoch.getDateTime(), 
						previousEpoch.getDateTime()).toMinutes());

		return minutes == 1;
	}
	
	public static class PossibleSleepPeriod{
		List<ActicalEpoch> epochs = new ArrayList<>();
		int epochListStartingIndex = -1;
	}
}
