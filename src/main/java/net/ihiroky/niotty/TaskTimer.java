package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface TaskTimer {
    void start();
    void stop();
    boolean hasTask();
    void offer(TaskLoop taskLoop, TaskLoop.Task task, long delay, TimeUnit timeUnit);

    TaskTimer NULL = new TaskTimer() {
        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean hasTask() {
            return false;
        }

        @Override
        public void offer(TaskLoop taskLoop, TaskLoop.Task task, long delay, TimeUnit timeUnit) {
        }

        @Override
        public String toString() {
            return "NULL_TIMER";
        }
    };
}
