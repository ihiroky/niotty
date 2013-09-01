package net.ihiroky.niotty;

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

    private final int numberOfThread_;
    private final String threadNamePrefix_;
    private State state_;
    private final Object stateLock_;

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
        numberOfThread_ = numberOfThread;
        threadNamePrefix_ = (threadNamePrefix != null) ? threadNamePrefix : DEFAULT_THREAD_NAME_PREFIX;
        state_ = State.INITIALIZED;
        stateLock_ = new Object();
    }

    @Override
    protected ThreadPipelineElementExecutor newTaskLoop() {
        return new ThreadPipelineElementExecutor(this);
    }

    @Override
    public PipelineElementExecutor assign(PipelineElement<?, ?> context) {
        synchronized (stateLock_) {
            if (state_ == State.INITIALIZED) {
                String prefix = threadNamePrefix_.concat(context.key().toString());
                super.open(new NameCountThreadFactory(prefix), numberOfThread_);
                state_ = State.OPEN;
            }
        }
        ThreadPipelineElementExecutor executor = super.assign(context);
        executor.accept(context);
        return executor;
    }

    @Override
    public void close() {
        synchronized (stateLock_) {
            if (state_ == State.OPEN) {
                super.close();
                state_ = State.CLOSED;
            }
        }
    }

    @Override
    public TaskLoopGroup<ThreadPipelineElementExecutor> taskLoopGroup() {
        return this;
    }
}
