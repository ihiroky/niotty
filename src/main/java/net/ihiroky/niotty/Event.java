package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * The event dispatched by the {@link EventDispatcher}.
 */
public interface Event {

    /**
     * The value returned by {@link #execute(java.util.concurrent.TimeUnit)}
     * and passed to {@link EventDispatcher#poll(long, java.util.concurrent.TimeUnit)}
     * to indicate that the thread should wait without timeout.
     */
    long DONE = -1;

    /**
     * The value returned by {@link #execute(java.util.concurrent.TimeUnit)}
     * and passed to {@link EventDispatcher#poll(long, java.util.concurrent.TimeUnit)}
     * to indicate that the thread should not wait.
     */
    long RETRY_IMMEDIATELY = 0L;

    /**
     * Executes the event procedure.
     * @param timeUnit unit of time returned by this method
     * @return {@link EventDispatcher DONE} if event is normally finished,
     *   {@link EventDispatcher RETRY_IMMEDIATELY} if event is required to execute again immediately,
     *   {@code timeUnit.convert(delay, unitOfDelay)} if event is required to execute again with the specified
     *   delay.
     * @throws Exception if some error occurs
     */
    long execute(TimeUnit timeUnit) throws Exception;
}
