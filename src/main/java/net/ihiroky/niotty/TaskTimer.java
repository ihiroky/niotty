package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface TaskTimer {
    void start();
    void stop();
    boolean hasTask();
    Entry offer(TaskLoop taskLoop, TaskLoop.Task task, long delay, TimeUnit timeUnit);

    interface Entry {
        void cancel();
        boolean isCancelled();
        boolean isDispatched();
    }
}
