package net.ihiroky.niotty;

import java.util.List;

/**
 * <p>An implementation of {@link PipelineElementExecutorPool} that manages
 * {@link ThreadPipelineElementExecutor}.</p>
 *
 * <p>This class use threads to execute the {@code ThreadPipelineElementExecutor}. </p>
 *
 * @author Hiroki Itoh
 */
public final class ThreadPipelineElementExecutorPool
        extends TaskLoopGroup<ThreadPipelineElementExecutor> implements PipelineElementExecutorPool {

    private final Object assignLock_;
    private final int numberOfThread_;
    private final String threadNamePrefix_;
    private State state_;

    private static final String DEFAULT_THREAD_NAME_PREFIX = "ExecutorFor";

    private enum State {
        INITIALIZED,
        OPEN,
        CLOSED,
    }

    /**
     * Constructs a instance.
     *
     * An invocation of this constructor behaves in exactly the same way as the invocation
     * {@code ThreadPipelineElementExecutorPool(numberOfThread, null)}.
     *
     * @param numberOfThread the number of the threads to be managed by the instance.
     */
    public ThreadPipelineElementExecutorPool(int numberOfThread) {
        this(numberOfThread, null);
    }

    /**
     * Constructs a instance.
     * @param numberOfThread the number of the threads to be managed by the instance.
     * @param threadNamePrefix a prefix of the thread name, "ExecutorFor" is used if null.
     */
    public ThreadPipelineElementExecutorPool(int numberOfThread, String threadNamePrefix) {
        if (numberOfThread <= 0) {
            throw new IllegalArgumentException("numberOfThread must be positive.");
        }
        assignLock_ = new Object();
        numberOfThread_ = numberOfThread;
        threadNamePrefix_ = (threadNamePrefix != null) ? threadNamePrefix : DEFAULT_THREAD_NAME_PREFIX;
        state_ = State.INITIALIZED;
    }

    @Override
    protected ThreadPipelineElementExecutor newEventLoop() {
        return new ThreadPipelineElementExecutor(this);
    }

    Object assignLock() {
        return assignLock_;
    }

    @Override
    public PipelineElementExecutor assign(PipelineElement<?, ?> context) {
        synchronized (assignLock_) {
            if (state_ == State.INITIALIZED) {
                String prefix = threadNamePrefix_.concat(context.key().toString());
                super.open(new NameCountThreadFactory(prefix), numberOfThread_);
                state_ = State.OPEN;
            }
            List<ThreadPipelineElementExecutor> loops = sortedLoopsView();
            ThreadPipelineElementExecutor target = loops.get(0);
            for (ThreadPipelineElementExecutor loop : loops) {
                if (loop.contains(context)) {
                    target = loop;
                    break;
                }
            }
            target.accept(context);
            return target;
        }
    }

    @Override
    public void close() {
        synchronized (assignLock_) {
            if (state_ == State.OPEN) {
                super.close();
                state_ = State.CLOSED;
            }
        }
    }
}
