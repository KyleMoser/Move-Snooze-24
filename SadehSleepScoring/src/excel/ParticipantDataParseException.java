package excel;

public class ParticipantDataParseException extends Exception {
	private static final long serialVersionUID = 1L;

	public ParticipantDataParseException(String message) {
        super(message);
    }

    public ParticipantDataParseException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
