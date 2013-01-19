package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * Created on 13/01/11, 15:31
 *
 * @author Hiroki Itoh
 */
public interface TransportFuture {

    Transport getTransport();
    boolean isCancelled();
    boolean isDone();
    Throwable getThrowable();
    void await() throws InterruptedException;
    void await(long timeout, TimeUnit unit) throws InterruptedException;
    void addListener();
    void removeListener();
}
