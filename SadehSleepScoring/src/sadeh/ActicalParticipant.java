package sadeh;

import ema.EMAPrompt;
import ema.EMAResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import analysis.SleepStats;

/**
 * A participant in the sleep data study. For a period of one week, this person's sleep data is collected
 * via a wrist worn device called an Actical. One piece of data is collected per minute; that piece of 
 * data is the level of movement (activity) for that minute (Epoch). 
 * 
 * @author kyle_
 *
 */
public class ActicalParticipant {
	//The identifier for the person whose data is being collected/analyzed
	protected String participant; 
	//All actical data collected during this study for the participant
	protected List<ActicalEpoch> sleepData = new ArrayList<>();
	//All EMA prompt (survey) data for the participant
	protected List<EMAPrompt> emaPrompts = new ArrayList<>();
	//We need to verify data validity; if the time period (e.g. day) is missing data points it must be excluded
	protected int numEpochsRequired = -1;
	//data is collected and analyzed for each patient at the baseline (initial collection), 6 months, and 12 months
	//and different reports may be produced depending on when the data for this epoch was collected.
	protected String assessmentPoint; 
	public List<EMAResult> results = new ArrayList<>();
	public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
	HashMap<String, List<ActicalEpoch>> dateEpochMap = new HashMap<>();
	HashMap<String, NapData> napMap = new HashMap<>();
	HashMap<String, SleepStats> sleepStats = new HashMap<>();
	
	public List<EMAPrompt> getEmaPrompts() {
		return emaPrompts;
	}

	public void setEmaPrompts(List<EMAPrompt> emaPrompts) {
		this.emaPrompts = emaPrompts;
	}

	/*
	 * Processes a list of EMAPrompts for the current participant.
	 * Note: assumes that the EMAPrompts passed in are all for the current participant.
	 */
	public void analyzeEmaData(List<EMAPrompt> prompts){		
		this.emaPrompts = prompts.stream().filter(p -> p.isResponsed()).collect(Collectors.toList());
		//List<EMAPrompt> asleepPrompts = this.emaPrompts.stream().filter(p -> p.isAsleep()).collect(Collectors.toList());
		//List<EMAPrompt> awakePrompts = this.emaPrompts.stream().filter(p -> !p.isAsleep()).collect(Collectors.toList());
		//analyzePrompts(asleepPrompts, true);
		analyzePrompts(this.emaPrompts, false);
	}
	
	public void analyzePrompts(List<EMAPrompt> prompts, boolean asleep){
		for (EMAPrompt prompt : prompts){
			List<ActicalEpoch> epochsBeforePrompt 
				= sleepData.stream().filter(
						p -> p.getDateTime().isBefore(prompt.getDateTime()) || p.getDateTime().isEqual(prompt.getDateTime()))
							.collect(Collectors.toList());
			epochsBeforePrompt = epochsBeforePrompt.stream().filter(p -> p.getDateTime().until(prompt.getDateTime(), 
					ChronoUnit.MINUTES) <= 9).collect(Collectors.toList());
			
			if (epochsBeforePrompt.size() < 10){
				System.out.println("EMA participant " + prompt.getParticipant() + " does not have enough Actical epochs data"
						+ " before the EMAPrompt " + prompt.asEpochDateTime());
			} else{
				int count = 0;
				
				for (ActicalEpoch e : epochsBeforePrompt){
					System.out.println("Participant: " + prompt.getParticipant() + ". EMA prompt (" + prompt.asEpochDateTime()
						+ "), asleep = " + prompt.isAsleep() + ". Actical epoch #" + count++ + " (" + e.asEpochDateTime()
						+ "), asleep = " + e.isAsleep());
				}
				
				EMAResult result = new EMAResult(prompt, epochsBeforePrompt);
				result.process();
				results.add(result);
				System.out.println(result);
			}
		}
	}
	
	public HashMap<String, NapData> getNapMap() {
		return napMap;
	}

	public void setNapMap(HashMap<String, NapData> napMap) {
		this.napMap = napMap;
	}

	public HashMap<String, SleepStats> getSleepStats() {
		return sleepStats;
	}

	public void setSleepStats(HashMap<String, SleepStats> sleepStats) {
		this.sleepStats = sleepStats;
	}

	public void addEpochToDateBasedMap(ActicalEpoch epoch){
		LocalDateTime ldt = epoch.getDateTime();
		String date = ldt.format(formatter);
		boolean hasKey = dateEpochMap.containsKey(date);
		if (hasKey){
			dateEpochMap.get(date).add(epoch);
		} else{
			List<ActicalEpoch> epochs = new ArrayList<>();
			epochs.add(epoch);
			dateEpochMap.put(date, epochs);
		}
	}
 	
	public void addSleepStatsToDateBasedMap(SleepStats stats){
		LocalDate ldt = stats.getLocalDate();
		String date = ldt.format(formatter);
		sleepStats.put(date, stats);
	}
	
	/**
	 * HashMap where the key is the date (day month year) and the value is a list of epochs collected that day
	 * @return
	 */
	public HashMap<String, List<ActicalEpoch>> getDateEpochMap() {
		return dateEpochMap;
	}
	public void setDateEpochMap(HashMap<String, List<ActicalEpoch>> dateEpochMap) {
		this.dateEpochMap = dateEpochMap;
	}
	public String getAssessmentPoint() {
		return assessmentPoint;
	}
	public void setAssessmentPoint(String assessmentPoint) {
		this.assessmentPoint = assessmentPoint;
	}
	
	public List<ActicalEpoch> getSleepData() {
		return sleepData;
	}
	public void setSleepData(List<ActicalEpoch> sleepData) {
		this.sleepData = sleepData;
	}
	public int getNumEpochsRequired() {
		return numEpochsRequired;
	}
	public void setNumEpochsRequired(int numEpochsRequired) {
		this.numEpochsRequired = numEpochsRequired;
	}
	public String getParticipant() {
		return participant;
	}
	public void setParticipant(String participant) {
		this.participant = participant;
	}
}
