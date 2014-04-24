package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * The event dispatched by the {@link EventDispatcher}.
 */
public interface Event {

    /**
     * The value returned by {@link #execute()}
     * and passed to {@link EventDispatcher#poll(long)}
     * to indicate that the thread should wait without timeout.
     */
    long DONE = -1;

    /**
     * The value returned by {@link #execute()}
     * and passed to {@link EventDispatcher#poll(long)}
     * to indicate that the thread should not wait.
     */
    long RETRY_IMMEDIATELY = 0L;

    /**
     * Executes the event procedure.
     * @return {@link EventDispatcher DONE} if event is normally finished,
     *   {@link EventDispatcher RETRY_IMMEDIATELY} if event is required to execute again immediately,
     *   timeout by nanoseconds if event is required to execute again with the specified delay.
     * @throws Exception if some error occurs
     */
    long execute() throws Exception;
}
