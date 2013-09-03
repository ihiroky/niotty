package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * The task executed by the {@link TaskLoop}.
 */
public interface Task {

    /**
     * The value returned by {@link #execute(java.util.concurrent.TimeUnit)}
     * and passed to {@link TaskLoop#poll(long, java.util.concurrent.TimeUnit)}
     * to indicate that the thread should wait without timeout.
     */
    long DONE = -1L;

    /**
     * The value returned by {@link #execute(java.util.concurrent.TimeUnit)}
     * and passed to {@link TaskLoop#poll(long, java.util.concurrent.TimeUnit)}
     * to indicate that the thread should not wait.
     */
    long RETRY_IMMEDIATELY = 0L;

    /**
     * Executes the task procedure.
     * @param timeUnit unit of time returned by this method
     * @return {@link TaskLoop DONE} if task is normally finished,
     *   {@link TaskLoop RETRY_IMMEDIATELY} if task is required to execute again immediately,
     *   {@code timeUnit.convert(delay, unitOfDelay)} if task is required to execute again with the specified
     *   delay.
     * @throws Exception if some error occurs
     */
    long execute(TimeUnit timeUnit) throws Exception;
}
