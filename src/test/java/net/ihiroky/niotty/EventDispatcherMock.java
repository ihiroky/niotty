package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
* @author Hiroki Itoh
*/
class EventDispatcherMock extends EventDispatcher {

    private final Lock lock_;
    private final Condition condition_;
    private volatile boolean signaled_;

    EventDispatcherMock() {
        lock_ = new ReentrantLock();
        condition_ = lock_.newCondition();
    }

    protected void onOpen() {
    }

    @Override
    protected void onClose() {
    }

    @Override
    protected void poll(long timeout, TimeUnit timeUnit) throws Exception {
        long timeoutNanos = timeUnit.toNanos(timeout);
        lock_.lock();
        try {
            while (!signaled_ && timeoutNanos > 0) {
                timeoutNanos = condition_.awaitNanos(timeoutNanos);
            }
            signaled_ = false;
        } finally {
            lock_.unlock();
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
