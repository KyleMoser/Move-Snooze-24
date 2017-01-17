package sadeh;

import java.util.ArrayList;
import java.util.List;

/**
 * A participant in the sleep data study. For a period of one week, this person's sleep data is collected
 * via a wrist worn device called an Actical. One piece of data is collected per minute; that piece of 
 * data is the level of movement (activity) for that minute (Epoch). 
 * 
 * @author kyle_
 *
 */
public class ParticipantData {
	//The identifier for the person whose data is being collected/analyzed
	protected String participant; 
	//All actical data collected during this study for the participant
	protected List<ActicalEpoch> sleepData = new ArrayList<>();
	//We need to verify data validity; if the time period (e.g. day) is missing data points it must be excluded
	protected int numEpochsRequired = -1;

	
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
