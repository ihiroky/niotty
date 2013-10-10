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

    /**
     * Constructs a instance.
     *
     * An invocation of this constructor behaves in exactly the same way as the invocation
     * <code>DefaultTaskLoopGroup(numberOfThread, Executors.defaultThreadFactory())</code>.
     *
     * @param workers the number of the threads to be managed by the instance
     */
    public DefaultTaskLoopGroup(int workers) {
        this(workers, Executors.defaultThreadFactory());
    }

    /**
     * Constructs a instance.
     *
     * @param workers the number of the threads to be managed by the instance
     * @param threadFactory a factory to create thread which runs a task loop
     */
    public DefaultTaskLoopGroup(int workers, ThreadFactory threadFactory) {
        super(threadFactory, workers);
    }

    @Override
    protected DefaultTaskLoop newTaskLoop() {
        return new DefaultTaskLoop();
    }
}
