package ema;
import java.util.List;
import java.util.ArrayList;
import sadeh.*;

public class EMAResult {
	public EMAPrompt prompt;
	List<ActicalEpoch> previousEpochsInclusive = new ArrayList<>();
	double proportionAsleep = -1.0;
	
	public EMAPrompt getPrompt() {
		return prompt;
	}

	public void setPrompt(EMAPrompt prompt) {
		this.prompt = prompt;
	}

	public List<ActicalEpoch> getPreviousEpochsInclusive() {
		return previousEpochsInclusive;
	}

	public void setPreviousEpochsInclusive(List<ActicalEpoch> previousEpochsInclusive) {
		this.previousEpochsInclusive = previousEpochsInclusive;
	}

	public double getProportionAsleep() {
		return proportionAsleep;
	}

	public void setProportionAsleep(double proportionAsleep) {
		this.proportionAsleep = proportionAsleep;
	}

	public EMAResult(EMAPrompt prompt, List<ActicalEpoch> prev){
		this.previousEpochsInclusive = prev;
		this.prompt = prompt;
	}
	
	public void process(){
		int countAsleep = 0;
		for (ActicalEpoch epoch : this.previousEpochsInclusive)
			if (epoch.isAsleep())
				countAsleep++;
		
		this.proportionAsleep = ((double)countAsleep/this.previousEpochsInclusive.size());
	}
	
	public String toString(){
		return "Participant " + prompt.getParticipant() + ", prompt (asleep = " + prompt.isAsleep() + ") at time " + prompt.asEpochDateTime() + ", proportion epochs asleep: " + getProportionAsleep();
	}
}
