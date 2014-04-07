package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * A implementation of {@link net.ihiroky.niotty.EventDispatcher} which depends on
 * {@link java.util.concurrent.locks.LockSupport#parkNanos(Object, long)} and
 * {@link java.util.concurrent.locks.LockSupport#unpark(Thread)} )}.
 */
public class DefaultEventDispatcher extends EventDispatcher {

    private volatile int signaled_;

    private static final AtomicIntegerFieldUpdater<DefaultEventDispatcher> SIGNALED_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(DefaultEventDispatcher.class, "signaled_");

    private static final int FALSE = 0;
    private static final int TRUE = 1;

    /**
     * Create a new instance.
     */
    public DefaultEventDispatcher() {
        signaled_ = FALSE;
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
        long timeoutNanos = timeUnit.convert(timeout, TimeUnit.NANOSECONDS);
        while (signaled_ == FALSE && timeoutNanos > 0L) {
            LockSupport.parkNanos(this, timeoutNanos);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            long now = System.nanoTime();
            timeoutNanos -= now - start;
            start = now;
        }
        signaled_ = FALSE;
    }

    @Override
    protected void wakeUp() {
        if (SIGNALED_UPDATER.compareAndSet(this, FALSE, TRUE)) {
            LockSupport.unpark(thread());
        }
    }
}
