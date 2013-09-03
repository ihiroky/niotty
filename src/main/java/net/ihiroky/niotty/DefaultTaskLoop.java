package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A implementation of {@link TaskLoop}. Wait operation depends on {@code java.util.ReentrantLock}.
 */
public class DefaultTaskLoop extends TaskLoop {

    private boolean signaled_;
    private final Lock lock_;
    private final Condition condition_;

    public DefaultTaskLoop() {
        super();
        signaled_ = false;
        lock_ = new ReentrantLock();
        condition_ = lock_.newCondition();
    }

    @Override
    protected void onOpen() {
    }

    @Override
    protected void onClose() {
    }

    @Override
    protected void poll(long timeout, TimeUnit timeUnit) throws InterruptedException {
        long timeoutNanos = timeUnit.toNanos(timeout);
        if (timeoutNanos > 0) {
            lock_.lock();
            try {
                while (!signaled_ && timeoutNanos <= 0) {
                    timeoutNanos = condition_.awaitNanos(timeoutNanos);
                }
                signaled_ = false;
            } finally {
                lock_.unlock();
            }
        } else if (timeoutNanos < 0) {
            lock_.lock();
            try {
                while (!signaled_) {
                    condition_.await();
                }
                signaled_ = false;
            } finally {
                lock_.unlock();
            }
        }
    }

    @Override
    protected void wakeUp() {
        lock_.lock();
        try {
            signaled_ = true;
            condition_.signal();
        } finally {
            lock_.unlock();
        }
    }
}
