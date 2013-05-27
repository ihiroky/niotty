package net.ihiroky.niotty;

import java.util.List;

/**
 * @author Hiroki Itoh
 */
public final class ThreadPipelineElementExecutorPool
        extends TaskLoopGroup<ThreadPipelineElementExecutor> implements PipelineElementExecutorPool {

    private final Object assignLock_;
    private final int numberOfThread_;
    private State state_;

    private enum State {
        INITIALIZED,
        OPEN,
        CLOSED,
    }

    public ThreadPipelineElementExecutorPool(int numberOfThread) {
        if (numberOfThread <= 0) {
            throw new IllegalArgumentException("numberOfThread must be positive.");
        }
        assignLock_ = new Object();
        numberOfThread_ = numberOfThread;
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
                super.open(new NameCountThreadFactory(context.key().toString()), numberOfThread_);
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
