package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Hiroki Itoh
 */
public class ThreadPipelineElementExecutor extends TaskLoop implements PipelineElementExecutor {

    private boolean signaled_;
    private final ThreadPipelineElementExecutorPool pool_;
    private final Lock lock_;
    private final Condition condition_;

    public ThreadPipelineElementExecutor(ThreadPipelineElementExecutorPool pool) {
        super();
        signaled_ = false;
        pool_ = pool;
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

    @Override
    public <I> void execute(final PipelineElement<I, ?> context, final I input) {
        execute(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                context.fire(input);
                return DONE;
            }
        });
    }

    @Override
    public <I> void execute(final PipelineElement<I, ?> context, final I input, final TransportParameter parameter) {
        execute(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                context.fire(input, parameter);
                return DONE;
            }
        });
    }

    @Override
    public void execute(final PipelineElement<?, ?> context, final TransportStateEvent event) {
        execute(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                context.fire(event);
                context.proceed(event);
                return DONE;
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
