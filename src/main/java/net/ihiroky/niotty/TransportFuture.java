package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * <p>A {@code TransportFuture} represents the result of the asynchronous I/O event.</p>
 * <p>A {@link CompletionListener} can be added to {@code TransportFuture}
 * to describe the operation after the result without blocking. The operation described
 * in the listener is called in the I/O thread.</p>
 */
public interface TransportFuture {

    /**
     * Returns the transport which is operated by the event associated with this future.
     *
     * @return the transport
     */
    Transport transport();

    /**
     * Returns true if the event associated with this future is executing.
     * The event is executing, but not done.
     *
     * @return true if the event associated with this future is executing
     */
    boolean isExecuting();

    /**
     * Returns true if the event is done.
     * To check the result status, use {@link #isSuccessful()}, {@link #isCancelled()}
     * and {@link #throwable()}.
     *
     * @return true if the event is done
     */
    boolean isDone();

    /**
     * Returns true if the event is done successfully.
     *
     * @return true if the event is done successfully
     */
    boolean isSuccessful();

    /**
     * Returns true if the event is cancelled.
     *
     * @return true if the event is cancelled
     */
    boolean isCancelled();

    /**
     * Returns a cause of a failure if the event is failed.
     *
     * @return the cause, or null if not failed.
     */
    Throwable throwable();

    /**
     * Throws a cause which is wrapped by {@code RuntimeException} of a failure
     * if the event is failed, or do nothing if not failed. If the cause is
     * {@code Error} or {@code RuntimeException}, they are thrown directly without
     * wrapping.
     */
    void throwRuntimeExceptionIfFailed();

    /**
     * Throws a cause of a failure if the event is failed, or do nothing if not failed.
     *
     * @throws Exception the cause
     */
    void throwExceptionIfFailed() throws Exception;

    /**
     * Requests the event to be cancelled if not started.
     *
     * @return true if the event is not started and signaled successfully
     */
    boolean cancel();

    /**
     * Causes the current thread to wait until the event is done or the current thread
     * is interrupted.
     * @return this future
     * @throws InterruptedException if the current thread is interrupted
     */
    TransportFuture await() throws InterruptedException;

    /**
     * Causes the current thread to wait until the event is done, the current thread
     * is interrupted or the specified timeout is elapsed.
     *
     * @param timeout the timeout
     * @param unit the unit of the timeout
     * @return true if the event is done within the timeout
     * @throws InterruptedException if the current thread is interrupted
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Causes the current thread to wait until the event is done. If the current thread
     * is interrupted, then {@code Thread.currentThread().interrupt()} is called.
     *
     * @return this future
     */
    TransportFuture awaitUninterruptibly();

    /**
     * Causes the current thread to wait until the event is done or the specified
     * timeout is elapsed. If the current thread is interrupted, then
     * {@code Thread.currentThread().interrupt()} is called.
     *
     * @param timeout the timeout
     * @param unit the unit of the timeout
     * @return true if the event is done within the timeout
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * Causes the current thread to wait until the event is done or any cause
     * to stop the event occurs. The cause is wrapped by {@link net.ihiroky.niotty.TransportException}.
     * If the event is cancelled, then {@code java.util.concurrent.CancellationException} is wrapped.
     *
     * @return this future
     * @throws net.ihiroky.niotty.TransportException if any cause to stop the event occurs.
     */
    TransportFuture join();

    /**
     * Causes the current thread to wait until the event is done, any cause
     * to stop the event occurs or the specified timeout is elapsed.
     * The cause is wrapped by {@link net.ihiroky.niotty.TransportException}.
     * If the event is cancelled, then {@code java.util.concurrent.CancellationException} is wrapped.
     * If the event is timed out, then {@code java.util.concurrent.TimeoutException} is wrapped.
     *
     * @return this future
     * @throws net.ihiroky.niotty.TransportException if any cause to stop the event occurs.
     */
    TransportFuture join(long timeout, TimeUnit unit);

    /**
     * Add the listener to receive this future if the event is done. The listener is
     * called in the I/O thread.
     *
     * @param listener the listener to be added
     * @return this future
     */
    TransportFuture addListener(CompletionListener listener);

    /**
     * Removes the listener.
     * @param listener the listener to be removed
     * @return this future
     */
    TransportFuture removeListener(CompletionListener listener);
}
