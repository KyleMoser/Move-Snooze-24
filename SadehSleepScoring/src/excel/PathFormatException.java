package excel;

public class PathFormatException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public PathFormatException(String message) {
        super(message);
    }

    public PathFormatException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
