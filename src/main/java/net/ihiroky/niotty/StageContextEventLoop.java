package net.ihiroky.niotty;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Hiroki Itoh
 */
public class StageContextEventLoop extends EventLoop<StageContextEventLoop> implements StageContextExecutor<Object> {

    private final Lock lock_;
    private final Condition condition_;
    private final AtomicBoolean signaled_;
    private final Set<StageContext<Object, ?>> contextSet_;
    private final Object contextMutex_;

    public StageContextEventLoop(Object contextMutex) {
        lock_ = new ReentrantLock();
        condition_ = lock_.newCondition();
        signaled_ = new AtomicBoolean();
        contextSet_ = new HashSet<>();
        contextMutex_ = contextSet_;
    }

    @Override
    protected void onOpen() {
    }

    @Override
    protected void onClose() {
    }

    @Override
    protected void process(int timeout) throws Exception {
        lock_.lock();
        try {
            long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeout);
            while (signaled_.getAndSet(false) || (timeoutNanos > 0 && hasNoTask())) {
                timeoutNanos = condition_.awaitNanos(timeoutNanos);
            }
        } finally {
            lock_.unlock();
        }
    }

    @Override
    protected void wakeUp() {
        lock_.lock();
        try {
            signaled_.set(true);
            condition_.signal();
        } finally {
            lock_.unlock();
        }
    }

    @Override
    public void execute(final StageContext<Object, ?> context, final Object input) {
        offerTask(new Task<StageContextEventLoop>() {
            @Override
            public boolean execute(StageContextEventLoop eventLoop) throws Exception {
                context.fire(input);
                return true;
            }
        });
    }

    @Override
    public void execute(final StageContext<Object, ?> context, final TransportStateEvent event) {
        offerTask(new Task<StageContextEventLoop>() {
            @Override
            public boolean execute(StageContextEventLoop eventLoop) throws Exception {
                context.fire(event);
                return true;
            }
        });
    }

    @Override
    public void invalidate(StageContext<Object, ?> context) {
        synchronized (contextMutex_) {
            contextSet_.remove(context);
        }
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    /**
     * Adds a specified {@code StageContext} to the context set.
     * This method must be called by {@link net.ihiroky.niotty.StageContextEventLoopGroup} only.
     *
     * @param context the {@code StageContext} to be added
     * @return true if the context set does not contains the {@code context}
     */
    boolean add(StageContext<Object, ?> context) {
        return contextSet_.add(context);
    }

    /**
     * Returns true if the context set contains a specified {@code StageContext}.
     * This method must be called by {@link net.ihiroky.niotty.StageContextEventLoopGroup} only.
     *
     * @param context the {@code StageContext} to be checked
     * @return true if the context set contains a specified {@code StageContext}.
     */
    boolean contains(StageContext<Object, ?> context) {
        return contextSet_.contains(context);
    }
}
