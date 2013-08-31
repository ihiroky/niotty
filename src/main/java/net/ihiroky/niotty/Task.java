package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * The task executed by the {@link TaskLoop}.
 */
public interface Task {
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
