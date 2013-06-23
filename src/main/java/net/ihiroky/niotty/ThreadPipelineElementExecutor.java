package net.ihiroky.niotty;

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
    private final ThreadPipelineElementExecutorPool pool_;

    public ThreadPipelineElementExecutor(ThreadPipelineElementExecutorPool pool) {
        lock_ = new ReentrantLock();
        condition_ = lock_.newCondition();
        signaled_ = false;
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
                context.proceed(event);
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
        reject(context);
    }
}
