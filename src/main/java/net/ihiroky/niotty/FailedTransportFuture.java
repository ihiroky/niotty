package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public class FailedTransportFuture extends CompletedTransportFuture {

    private Throwable throwable;

    public FailedTransportFuture(Transport transport, Throwable throwable) {
        super(transport);

        this.throwable = throwable;
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public void throwIfFailed() {
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        } else if (throwable instanceof Error) {
            throw (Error) throwable;
        } else {
            throw new RuntimeException(throwable);
        }
    }
}
