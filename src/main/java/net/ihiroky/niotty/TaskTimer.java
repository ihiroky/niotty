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
        public Entry offer(TaskLoop taskLoop, TaskLoop.Task task, long delay, TimeUnit timeUnit) {
            return null;
        }

        @Override
        public String toString() {
            return "NULL_TIMER";
        }
    };
}
