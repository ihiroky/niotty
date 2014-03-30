package net.ihiroky.niotty;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * <p>An implementation of {@link net.ihiroky.niotty.EventDispatcherGroup}
 * that manages {@link net.ihiroky.niotty.DefaultEventDispatcher}.</p>
 *
 * <p>This class use threads to execute the {@code DefaultEventDispatcher}. </p>
 */
public final class DefaultEventDispatcherGroup
        extends EventDispatcherGroup<DefaultEventDispatcher> {

    /**
     * Constructs a instance.
     *
     * An invocation of this constructor behaves in exactly the same way as the invocation
     * <code>DefaultEventDispatcherGroup(numberOfThread, Executors.defaultThreadFactory())</code>.
     *
     * @param workers the number of the threads to be managed by the instance
     */
    public DefaultEventDispatcherGroup(int workers) {
        this(workers, Executors.defaultThreadFactory());
    }

    /**
     * Constructs a instance.
     *
     * @param workers the number of the threads to be managed by the instance
     * @param threadFactory a factory to create thread which runs a event dispatcher
     */
    public DefaultEventDispatcherGroup(int workers, ThreadFactory threadFactory) {
        super(threadFactory, workers);
    }

    @Override
    protected DefaultEventDispatcher newEventDispatcher() {
        return new DefaultEventDispatcher();
    }
}
