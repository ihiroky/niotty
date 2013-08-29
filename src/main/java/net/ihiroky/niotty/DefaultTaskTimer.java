package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public class DefaultTaskTimer implements Runnable, TaskTimer {

    private final ConcurrentLinkedQueue<TimerFuture> queue_;
    private final String name_;
    private final ReentrantLock lock_;
    private final Condition condition_;
    private volatile Thread thread_;
    private volatile boolean hasNoTimer_;
    private int retainCount_;

    private Logger logger_ = LoggerFactory.getLogger(DefaultTaskTimer.class);

    private static final int DEFAULT_INITIAL_CAPACITY = 1024;

    public static final TaskTimer NULL = new TaskTimer() {
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
        public Future offer(TaskLoop taskLoop, TaskLoop.Task task, long delay, TimeUnit timeUnit) {
            return null;
        }

        @Override
        public String toString() {
            return "NULL_TIMER";
        }
    };

    public DefaultTaskTimer(String name) {
        queue_ = new ConcurrentLinkedQueue<>();
        lock_ = new ReentrantLock();
        condition_ = lock_.newCondition();
        name_ = String.valueOf(name);
        hasNoTimer_ = true;
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
    public Future offer(TaskLoop taskLoop, TaskLoop.Task task, long delay, TimeUnit timeUnit) {
        TimerFuture e = new TimerFuture(System.nanoTime() + timeUnit.toNanos(delay), taskLoop, task);
        queue_.offer(e);
        hasNoTimer_ = false;

        lock_.lock();
        try {
            condition_.signal();
        } finally {
            lock_.unlock();
        }

        return e;
    }

    @Override
    public void run() {
        try {
            logger_.info("[run] Start timer thread:{}", Thread.currentThread().getName());

            final PriorityQueue<TimerFuture> timerQueue = new PriorityQueue<>(DEFAULT_INITIAL_CAPACITY);
            long waitNanos = Long.MAX_VALUE;
            while (thread_ != null) {
                // Check if new timer is exists and register the timer to timer queue
                lock_.lock();
                try {
                    while (waitNanos > 0 && queue_.isEmpty()) {
                        waitNanos = condition_.awaitNanos(waitNanos);
                    }
                } finally {
                    lock_.unlock();
                }
                while (!queue_.isEmpty()) {
                    timerQueue.offer(queue_.poll());
                }

                // Check timeout or cancel.
                long now = System.nanoTime();
                TimerFuture e = timerQueue.peek();
                while (e != null && e.isCancelled()) {
                    timerQueue.poll();
                    e = timerQueue.peek();
                }
                hasNoTimer_ = timerQueue.isEmpty();
                waitNanos = (e != null) ? e.expire_ - now : Long.MAX_VALUE;
                if (waitNanos > 0) {
                    continue;
                }

                // Execute expired timers.
                for (;;) {
                    e = timerQueue.peek();
                    if (e == null || e.expire_ > now || !e.readyToDispatch()) {
                        break;
                    }
                    e.taskLoop_.offerTask(e.task_);
                    e.done();
                    timerQueue.poll();
                }
                while (e != null && e.isCancelled()) {
                    timerQueue.poll();
                    e = timerQueue.peek();
                }
                hasNoTimer_ = timerQueue.isEmpty();
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
        return !hasNoTimer_;
    }

    public boolean isAlive() {
        return thread_ != null && thread_.isAlive();
    }

    public static class TimerFuture implements Future, Comparable<TimerFuture> {
        private final long expire_;
        private final TaskLoop taskLoop_;
        private final TaskLoop.Task task_;
        private volatile int state_ = WAITING;

        private static final AtomicIntegerFieldUpdater<TimerFuture> STATE_UPDATER =
                AtomicIntegerFieldUpdater.newUpdater(TimerFuture.class, "state_");

        private static final int WAITING = 0;
        private static final int READY = 1;
        private static final int DISPATCHED = 2;
        private static final int CANCELLED = 3;

        TimerFuture(long expire, TaskLoop taskLoop, TaskLoop.Task task) {
            expire_ = expire;
            taskLoop_ = taskLoop;
            task_ = task;

        }

        boolean readyToDispatch() {
            return STATE_UPDATER.compareAndSet(this, WAITING, READY);
        }

        void done() {
            state_ = DISPATCHED;
        }

        @Override
        public void cancel() {
            STATE_UPDATER.compareAndSet(this, WAITING, CANCELLED);
        }

        @Override
        public boolean isCancelled() {
            return state_ == CANCELLED;
        }

        @Override
        public boolean isDispatched() {
            return state_ == DISPATCHED;
        }

        @Override
        public int compareTo(TimerFuture o) {
            return (expire_ > o.expire_)
                    ? 1
                    : ((expire_ < o.expire_) ? -1 : 0);
        }

        @Override
        public String toString() {
            return "(expire:" + expire_ + ", task loop:" + taskLoop_
                    + ", task:" + task_ + ", cancelled:" + isCancelled() + ")";
        }
    }
}
