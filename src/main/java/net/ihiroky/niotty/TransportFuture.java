package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 */
public interface TransportFuture {

    Transport transport();
    boolean isExecuting();
    boolean isDone();
    boolean isSuccessful();
    boolean isCancelled();
    Throwable throwable();
    void throwRuntimeExceptionIfFailed();
    void throwExceptionIfFailed() throws Exception;
    boolean cancel();
    TransportFuture waitForCompletion() throws InterruptedException;
    TransportFuture waitForCompletion(long timeout, TimeUnit unit) throws InterruptedException;
    TransportFuture waitForCompletionUninterruptibly();
    TransportFuture waitForCompletionUninterruptibly(long timeout, TimeUnit unit);
    TransportFuture addListener(CompletionListener listener);
    TransportFuture removeListener(CompletionListener listener);
}
