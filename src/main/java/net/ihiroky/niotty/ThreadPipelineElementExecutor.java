package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * @author Hiroki Itoh
 */
public class ThreadPipelineElementExecutor extends TaskLoop implements PipelineElementExecutor {

    private boolean signaled_;
    private final ThreadPipelineElementExecutorPool pool_;
    private final Object lock_;

    public ThreadPipelineElementExecutor(ThreadPipelineElementExecutorPool pool) {
        super(DefaultTaskTimer.NULL); // No task uses the timer.
        signaled_ = false;
        pool_ = pool;
        lock_ = new Object();
    }

    @Override
    protected void onOpen() {
    }

    @Override
    protected void onClose() {
    }

    @Override
    protected void poll(boolean preferToWait) throws InterruptedException {
        if (preferToWait) {
            synchronized (lock_) {
                while (!signaled_) {
                    wait();
                }
            }
        }
    }

    @Override
    protected void wakeUp() {
        synchronized (lock_) {
            signaled_ = true;
            notify();
        }
    }

    @Override
    public <I> void execute(final PipelineElement<I, ?> context, final I input) {
        offerTask(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                context.fire(input);
                return DONE;
            }
        });
    }

    @Override
    public <I> void execute(final PipelineElement<I, ?> context, final I input, final TransportParameter parameter) {
        offerTask(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                context.fire(input, parameter);
                return DONE;
            }
        });
    }

    @Override
    public void execute(final PipelineElement<?, ?> context, final TransportStateEvent event) {
        offerTask(new Task() {
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
