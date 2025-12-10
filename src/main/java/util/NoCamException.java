package util;

public class NoCamException extends Exception {
    public NoCamException(String message) {
        super(message);
    }

    public NoCamException(String message, Throwable cause) {
        super(message, cause);
    }
}
