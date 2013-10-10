package net.ihiroky.niotty;

/**
 * Unchecked exception thrown when an I/O related exception occurs.
 */
public class TransportException extends RuntimeException {

    private static final long serialVersionUID = -5574299685420739740L;

    /**
     * Creates an instance.
     * @param message the message
     */
    public TransportException(String message) {
        super(message);
    }

    /**
     * Creates an instance.
     * @param cause the cause
     */
    public TransportException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance.
     * @param message the message
     * @param cause the cause
     */
    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
