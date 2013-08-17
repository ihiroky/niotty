package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public class DefaultTaskTimer implements Runnable, TaskTimer {

    private final PriorityQueue<TimerEntry> queue_;
    private final String name_;
    private final ReentrantLock lock_;
    private final Condition condition_;
    private volatile Thread thread_;
    private int retainCount_;

    private Logger logger_ = LoggerFactory.getLogger(DefaultTaskTimer.class);

    private static final int DEFAULT_INITIAL_CAPACITY = 1024;

    public DefaultTaskTimer(String name) {
        this(name, DEFAULT_INITIAL_CAPACITY);
    }

    public DefaultTaskTimer(String name, int initialCapacity) {
        queue_ = new PriorityQueue<>(initialCapacity);
        lock_ = new ReentrantLock();
        condition_ = lock_.newCondition();
        name_ = String.valueOf(name);
    }

    @Override
    public synchronized void start() {
        if (thread_ == null) {
            Thread t = new Thread(this, "Timer-" + name_);
            t.start();
            thread_ = t;
        }
        retainCount_++;
    }

    @Override
    public synchronized void stop() {
        if (thread_ != null && --retainCount_ == 0) {
            thread_.interrupt();
            thread_ = null;
        }
    }

    @Override
    public void offer(TaskLoop taskLoop, TaskLoop.Task task, long delay, TimeUnit timeUnit) {
        lock_.lock();
        try {
            queue_.offer(new TimerEntry(System.nanoTime() + timeUnit.toNanos(delay), taskLoop, task));
            condition_.signal();
        } finally {
            lock_.unlock();
        }
    }

    @Override
    public void run() {
        try {
            logger_.info("[run] Start timer thread:{}", Thread.currentThread().getName());
            while (thread_ != null) {
                List<TimerEntry> expiredList = Collections.emptyList();
                lock_.lock();
                try {
                    LOOP: for (long waitNanos;;) {
                        long now = System.nanoTime();
                        TimerEntry e = queue_.peek();
                        waitNanos = (e != null) ? e.expire_ - now : Long.MAX_VALUE;
                        if (waitNanos > 0) {
                            condition_.awaitNanos(waitNanos);
                            continue;
                        }
                        expiredList = new ArrayList<>();
                        for (;;) {
                            expiredList.add(queue_.poll());
                            e = queue_.peek();
                            if (e == null || e.expire_ > now) {
                                break LOOP;
                            }
                        }
                    }
                } finally {
                    lock_.unlock();
                }

                logger_.debug("[run] expired entries:{}.", expiredList);
                for (TimerEntry e : expiredList) {
                    e.taskLoop_.offerTask(e.task_);
                }
            }
        } catch (RuntimeException re) {
            logger_.error("[run] Unexpected exception.", re);
        } catch (InterruptedException ie) {
            logger_.debug("[run] Interrupted.", ie);
        } finally {
            logger_.info("[run] Stop timer thread:{}", Thread.currentThread().getName());
        }
    }

    @Override
    public boolean hasTask() {
        return !queue_.isEmpty();
    }

    public boolean isAlive() {
        return thread_ != null && thread_.isAlive();
    }

    private static class TimerEntry implements Comparable<TimerEntry> {
        final long expire_;
        final TaskLoop taskLoop_;
        final TaskLoop.Task task_;

        TimerEntry(long expire, TaskLoop taskLoop, TaskLoop.Task task) {
            expire_ = expire;
            taskLoop_ = taskLoop;
            task_ = task;
        }

        @Override
        public int compareTo(TimerEntry o) {
            return (expire_ > o.expire_)
                    ? 1
                    : ((expire_ < o.expire_) ? -1 : 0);
        }

        @Override
        public String toString() {
            return "(expire:" + expire_ + ", task loop:" + taskLoop_ + ", task:" + task_ + ")";
        }
    }
}
