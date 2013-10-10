package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TaskLoopGroup;

import java.util.concurrent.ThreadFactory;

/**
 * An implementation of {@link AbstractSelector} to handle asynchronous connections.
 */
public class ConnectSelectorPool extends TaskLoopGroup<ConnectSelector> {

    /**
     * Constructs a new instance.
     *
     * @param threadFactory a factory to create thread which runs a task loop
     * @param workers       the number of threads held in the thread pool
     */
    protected ConnectSelectorPool(ThreadFactory threadFactory, int workers) {
        super(threadFactory, workers);
    }

    @Override
    protected ConnectSelector newTaskLoop() {
        return new ConnectSelector();
    }
}
