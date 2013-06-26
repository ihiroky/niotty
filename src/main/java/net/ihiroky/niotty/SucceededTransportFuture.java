package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public class SucceededTransportFuture extends CompletedTransportFuture {

    public SucceededTransportFuture(Transport transport) {
        super(transport);
    }

    @Override
    public boolean isSuccessful() {
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
