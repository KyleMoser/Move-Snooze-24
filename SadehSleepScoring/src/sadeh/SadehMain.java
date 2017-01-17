package sadeh;

import java.io.File;
import java.util.Collections;
import java.util.List;
import parser.ActicalExcelParser;

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
		try {
			String participant = getParticipantName(path);
			System.out.println(participant);
			List<ActicalEpoch> participantEpochs = ActicalExcelParser.parseSadehExcelDocument(path, "baseline");
			Collections.sort(participantEpochs, (a, b) -> a.getDateTime().compareTo(b.getDateTime()));
			participantEpochs.forEach(epoch -> System.out.println(epoch));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String getParticipantName(String path){
		File file = new File(path);
		String[] names = file.getName().split("\\.");
		return names[0];
	}
	
}
