package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public class SuccessfulTransportFuture extends CompletedTransportFuture {

    public SuccessfulTransportFuture(Transport transport) {
        super(transport);
    }

    @Override
    public boolean isSuccessful() {
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
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
