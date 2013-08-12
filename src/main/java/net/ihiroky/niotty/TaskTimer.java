package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface TaskTimer {
    void start();
    void stop();
    void offer(TaskLoop taskLoop, TaskLoop.Task task, long delay, TimeUnit timeUnit);
    long flush(TimeUnit timeUnit);
}
