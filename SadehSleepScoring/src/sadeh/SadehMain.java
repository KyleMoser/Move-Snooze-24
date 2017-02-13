package sadeh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import excel.ActicalDataOutputException;
import excel.ActicalExcelParser;
import excel.ActivityThresholdWorkbook;
import excel.ParticipantDataParseException;

import java.util.Set;
import java.util.stream.Stream;

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
			
			try(Stream<Path> paths = Files.walk(Paths.get(inputPath), 1)) {
			    paths.forEach(filePath -> {
			        if (Files.isRegularFile(filePath)) {
			            System.out.println(filePath);
			            process(filePath.toFile(), assessmentPoint, outputPath);
			        }
			    });
			} catch (Exception ex){
				System.out.println(ex.getMessage());
			}
		} catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
	
	private static String getParticipantName(File file){
		String[] names = file.getName().split("\\.");
		return names[0];
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
			
			if ((!napping && consecutiveNappingEpochs >= 30) || i == epochs.size()-1){
				naps.add(consecutiveNappingEpochs);
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
	
	public static void process(File excel, String assessmentPoint, String outputPath){
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
			
			ActivityThresholdWorkbook atw = new ActivityThresholdWorkbook(epochs);
			atw.create();
			atw.write(outputPath + "\\" + name + "_" + assessmentPoint + ".xlsx");
		} catch (ParticipantDataParseException e) {
			//errorReport.add(e.getMessage());
			System.out.println(e.getMessage());
		} catch (ActicalDataOutputException e) {
			System.out.println(e.getMessage());
		}
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
}
