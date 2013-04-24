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
public class ThreadStageContextExecutor extends TaskLoop<ThreadStageContextExecutor> implements StageContextExecutor {

    private final Lock lock_;
    private final Condition condition_;
    private final AtomicBoolean signaled_;
    private final Set<StageContext<?, ?>> contextSet_;
    private final ThreadStageContextExecutorPool pool_;

    public ThreadStageContextExecutor(ThreadStageContextExecutorPool pool) {
        lock_ = new ReentrantLock();
        condition_ = lock_.newCondition();
        signaled_ = new AtomicBoolean();
        contextSet_ = new HashSet<>();
        pool_ = pool;
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
    public <I> void execute(final StageContext<I, ?> context, final I input) {
        offerTask(new Task<ThreadStageContextExecutor>() {
            @Override
            public int execute(ThreadStageContextExecutor eventLoop) throws Exception {
                context.fire(input);
                return TIMEOUT_NO_LIMIT;
            }
        });
    }

    @Override
    public <I> void execute(final StageContext<I, ?> context, final TransportStateEvent event) {
        offerTask(new Task<ThreadStageContextExecutor>() {
            @Override
            public int execute(ThreadStageContextExecutor eventLoop) throws Exception {
                context.fire(event);
                return TIMEOUT_NO_LIMIT;
            }
        });
    }

    @Override
    public StageContextExecutorPool pool() {
        return pool_;
    }

    @Override
    public void close(StageContext<?, ?> context) {
        synchronized (pool_.assignLock()) {
            contextSet_.remove(context);
        }
        processingMemberCount_.decrementAndGet();
    }

    /**
     * Adds a specified {@code StageContext} to the context set.
     * This method can be called by {@link ThreadStageContextExecutorPool} only.
     *
     * @param context the {@code StageContext} to be added
     * @return true if the context set does not contains the {@code context}
     */
    boolean accept(StageContext<?, ?> context) {
        processingMemberCount_.incrementAndGet();
        return contextSet_.add(context);
    }

    /**
     * Returns true if the context set contains a specified {@code StageContext}.
     * This method can be called by {@link ThreadStageContextExecutorPool} only.
     *
     * @param context the {@code StageContext} to be checked
     * @return true if the context set contains a specified {@code StageContext}.
     */
    boolean contains(StageContext<?, ?> context) {
        return contextSet_.contains(context);
    }
}
