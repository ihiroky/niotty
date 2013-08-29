package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface TaskTimer {
    void start();
    void stop();
    boolean hasTask();
    Future offer(TaskLoop taskLoop, TaskLoop.Task task, long delay, TimeUnit timeUnit);

    interface Future {
        void cancel();
        boolean isCancelled();
        boolean isDispatched();
    }
}
