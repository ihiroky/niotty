package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public class FailedTransportFuture extends CompletedTransportFuture {

    private Throwable throwable_;

    public FailedTransportFuture(Transport transport, Throwable throwable) {
        super(transport);

        this.throwable_ = throwable;
    }

    @Override
    public Throwable throwable() {
        return throwable_;
    }

    @Override
    public void throwIfFailed() {
        if (throwable_ instanceof RuntimeException) {
            throw (RuntimeException) throwable_;
        } else if (throwable_ instanceof Error) {
            throw (Error) throwable_;
        } else {
            throw new RuntimeException(throwable_);
        }
    }
}
