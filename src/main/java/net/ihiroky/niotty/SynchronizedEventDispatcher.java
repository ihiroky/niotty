package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * A implementation of {@link net.ihiroky.niotty.EventDispatcher}. Wait operation depends on
 * {@code Object.wait()} and {@code Object.notify()}.
 */
public class SynchronizedEventDispatcher extends EventDispatcher {

    private boolean signaled_;
    private final Object lock_;

    /**
     * Create a new instance.
     */
    public SynchronizedEventDispatcher() {
        signaled_ = false;
        lock_ = new Object();
    }

    @Override
    protected void onOpen() {
    }

    @Override
    protected void onClose() {
    }

    @Override
    protected void poll(long timeout, TimeUnit timeUnit) throws InterruptedException {
        long start = System.nanoTime();
        synchronized (lock_) {
            while (!signaled_ && timeout > 0) {
                timeUnit.timedWait(lock_, timeout);
                long now = System.nanoTime();
                timeout -= timeUnit.convert(now - start, TimeUnit.NANOSECONDS);
                start = now;
            }
            signaled_ = false;
        }
    }

    @Override
    protected void wakeUp() {
        synchronized (lock_) {
            if (!signaled_) {
                signaled_ = true;
                lock_.notify();
            }
        }
    }
}
