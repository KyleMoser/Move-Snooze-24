package sadeh;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import parser.ActicalExcelParser;
import parser.ParticipantDataParseException;

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
		String path = args[0];
		List<String> errorReport = new ArrayList<String>();
		
		try {
			File excel = new File(path);
			List<ActicalEpoch> participantEpochs = ActicalExcelParser.parseSadehExcelDocument(excel, "baseline");
			//Sort the Actical data by date, earlier dates first
			Collections.sort(participantEpochs, (a, b) -> a.getDateTime().compareTo(b.getDateTime()));
			participantEpochs.forEach(epoch -> System.out.println(epoch));
			
			for (int i = 0; i < participantEpochs.size(); i++){
				if (!validateEpoch(participantEpochs, i))
					throw new ParticipantDataParseException("File format is invalid for the file " + excel.getAbsolutePath()
							+ ", it must be manually processed.");
				SadehSleepAlgorithm.sadeh(participantEpochs, i);
			}
		} catch (ParticipantDataParseException e) {
			errorReport.add(e.getMessage());
			System.out.println(e.getMessage());
		}
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
