package net.ihiroky.niotty;

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
     * Creates a new instance.
     *
     * @param eventQueueCapacity the capacity of the event queue to buffer events;
     *                           less than or equal 0 to use unbounded queue
     */
    public DefaultEventDispatcher(int eventQueueCapacity) {
        super(eventQueueCapacity);
        signaled_ = FALSE;
    }

    @Override
    protected void onOpen() {
    }

    @Override
    protected void onClose() {
    }

    @Override
    protected void poll(long timeoutNanos) throws InterruptedException {
        long start = System.nanoTime();
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
