package net.ihiroky.niotty;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * <p>An implementation of {@link TaskLoopGroup} that manages {@link DefaultTaskLoop}.</p>
 *
 * <p>This class use threads to execute the {@code DefaultTaskLoop}. </p>
 */
public final class DefaultTaskLoopGroup
        extends TaskLoopGroup<DefaultTaskLoop> {

    private final int numberOfThread_;
    private final String threadNamePrefix_;
    private State state_;
    private final Object stateLock_;

    private enum State {
        INITIALIZED,
        OPEN,
        CLOSED,
    }

    /**
     * Constructs a instance.
     *
     * An invocation of this constructor behaves in exactly the same way as the invocation
     * {@code DefaultTaskLoopGroup(numberOfThread, null)}.
     *
     * @param numberOfThread the number of the threads to be managed by the instance.
     */
    public DefaultTaskLoopGroup(int numberOfThread) {
        this(numberOfThread, null);
    }

    /**
     * Constructs a instance.
     * @param numberOfThread the number of the threads to be managed by the instance.
     * @param threadNamePrefix a prefix of the thread name, "ExecutorFor" is used if null.
     */
    public DefaultTaskLoopGroup(int numberOfThread, String threadNamePrefix) {
        if (numberOfThread <= 0) {
            throw new IllegalArgumentException("numberOfThread must be positive.");
        }
        numberOfThread_ = numberOfThread;
        threadNamePrefix_ = threadNamePrefix;
        state_ = State.INITIALIZED;
        stateLock_ = new Object();
    }

    @Override
    public void open(ThreadFactory threadFactory, int numberOfThread) {
        synchronized (stateLock_) {
            if (state_ == State.INITIALIZED) {
                super.open(threadFactory, numberOfThread);
                state_ = State.OPEN;
            }
        }
    }

    @Override
    protected DefaultTaskLoop newTaskLoop() {
        return new DefaultTaskLoop();
    }

    @Override
    public DefaultTaskLoop assign(TaskSelection context) {
        synchronized (stateLock_) {
            if (state_ == State.INITIALIZED) {
                ThreadFactory threadFactory = (threadNamePrefix_ != null)
                        ? new NameCountThreadFactory(threadNamePrefix_) : Executors.defaultThreadFactory();
                open(threadFactory, numberOfThread_);
                state_ = State.OPEN;
            }
        }
        return super.assign(context);
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
}
