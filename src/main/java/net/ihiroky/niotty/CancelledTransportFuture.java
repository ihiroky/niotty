package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public class CancelledTransportFuture extends CompletedTransportFuture {


    public CancelledTransportFuture(AbstractTransport<?> transport) {
        super(transport);
    }

    @Override
    public boolean isSuccessful() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return true;
    }

    @Override
    public Throwable throwable() {
        return null;
    }

    @Override
    public void throwRuntimeExceptionIfFailed() {
    }

    @Override
    public void throwExceptionIfFailed() throws Exception {
    }
}
