package net.ihiroky.niotty;

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
        super(0);
        lock_ = new ReentrantLock();
        condition_ = lock_.newCondition();
    }

    protected void onOpen() {
    }

    @Override
    protected void onClose() {
    }

    @Override
    protected void poll(long timeoutNanos) throws Exception {
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
