package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public class FailedTransportFuture extends CompletedTransportFuture {

    private Throwable throwable_;

    public FailedTransportFuture(AbstractTransport<?> transport, Throwable throwable) {
        super(transport);

        this.throwable_ = throwable;
    }

    @Override
    public boolean isSuccessful() {
        return false;
    }

    @Override
    public Throwable throwable() {
        return throwable_;
    }

    @Override
    public void throwRuntimeExceptionIfFailed() {
        if (throwable_ instanceof RuntimeException) {
            throw (RuntimeException) throwable_;
        } else if (throwable_ instanceof Exception) {
            throw new RuntimeException(throwable_);
        } else if (throwable_ instanceof Error) {
            throw (Error) throwable_;
        }
        throw new AssertionError("Unexpected throwable", throwable_);
    }

    @Override
    public void throwExceptionIfFailed() throws Exception {
        if (throwable_ != null) {
            if (throwable_ instanceof RuntimeException) {
                throw (RuntimeException) throwable_;
            } else if (throwable_ instanceof Exception) {
                throw (Exception) throwable_;
            } else if (throwable_ instanceof Error) {
                throw (Error) throwable_;
            }
            throw new AssertionError("Unexpected throwable", throwable_);
        }
    }
}
