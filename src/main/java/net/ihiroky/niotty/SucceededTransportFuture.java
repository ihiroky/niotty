package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public class SucceededTransportFuture extends CompletedTransportFuture {

    public SucceededTransportFuture(Transport transport) {
        super(transport);
    }

    @Override
    public Throwable getThrowable() {
        return null;
    }

    @Override
    public void throwIfFailed() {
    }
}
