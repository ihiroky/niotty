package net.ihiroky.niotty;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Hiroki Itoh
 */
public class ThreadPipelineElementExecutor extends TaskLoop<ThreadPipelineElementExecutor> implements PipelineElementExecutor {

    private final Lock lock_;
    private final Condition condition_;
    private boolean signaled_;
    private final Set<PipelineElement<?, ?>> contextSet_;
    private final ThreadPipelineElementExecutorPool pool_;

    public ThreadPipelineElementExecutor(ThreadPipelineElementExecutorPool pool) {
        lock_ = new ReentrantLock();
        condition_ = lock_.newCondition();
        signaled_ = false;
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
    protected void process(int timeout) throws InterruptedException {
        lock_.lock();
        try {
            if (timeout > 0) {
                long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeout);
                while (!signaled_) {
                    timeoutNanos = condition_.awaitNanos(timeoutNanos);
                }
            } else if (timeout < 0) {
                while (!signaled_) {
                    condition_.await();
                }
            }
        } finally {
            signaled_ = false;
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

    @Override
    public <I> void execute(final PipelineElement<I, ?> context, final I input) {
        offerTask(new Task<ThreadPipelineElementExecutor>() {
            @Override
            public int execute(ThreadPipelineElementExecutor eventLoop) throws Exception {
                context.fire(input);
                return TIMEOUT_NO_LIMIT;
            }
        });
    }

    @Override
    public <I> void execute(final PipelineElement<I, ?> context, final I input, final TransportParameter parameter) {
        offerTask(new Task<ThreadPipelineElementExecutor>() {
            @Override
            public int execute(ThreadPipelineElementExecutor eventLoop) throws Exception {
                context.fire(input, parameter);
                return TIMEOUT_NO_LIMIT;
            }
        });
    }

    @Override
    public void execute(final PipelineElement<?, ?> context, final TransportStateEvent event) {
        offerTask(new Task<ThreadPipelineElementExecutor>() {
            @Override
            public int execute(ThreadPipelineElementExecutor eventLoop) throws Exception {
                context.fire(event);
                return TIMEOUT_NO_LIMIT;
            }
        });
    }

    @Override
    public PipelineElementExecutorPool pool() {
        return pool_;
    }

    @Override
    public void close(PipelineElement<?, ?> context) {
        synchronized (pool_.assignLock()) {
            contextSet_.remove(context);
        }
        processingMemberCount_.decrementAndGet();
    }

    /**
     * Adds a specified {@code PipelineElement} to the context set.
     * This method can be called by {@link ThreadPipelineElementExecutorPool} only.
     *
     * @param context the {@code PipelineElement} to be added
     * @return true if the context set does not contains the {@code context}
     */
    boolean accept(PipelineElement<?, ?> context) {
        processingMemberCount_.incrementAndGet();
        return contextSet_.add(context);
    }

    /**
     * Returns true if the context set contains a specified {@code PipelineElement}.
     * This method can be called by {@link ThreadPipelineElementExecutorPool} only.
     *
     * @param context the {@code PipelineElement} to be checked
     * @return true if the context set contains a specified {@code PipelineElement}.
     */
    boolean contains(PipelineElement<?, ?> context) {
        return contextSet_.contains(context);
    }
}
