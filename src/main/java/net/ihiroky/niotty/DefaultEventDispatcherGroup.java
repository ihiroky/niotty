package net.ihiroky.niotty;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * <p>An implementation of {@link net.ihiroky.niotty.EventDispatcherGroup}
 * that manages {@link net.ihiroky.niotty.DefaultEventDispatcher}.</p>
 *
 * <p>This class use threads to execute the {@code DefaultEventDispatcher}. </p>
 */
public final class DefaultEventDispatcherGroup extends EventDispatcherGroup {

    private static class DefaultEventDispatcherFactory implements EventDispatcherFactory<DefaultEventDispatcher> {

        private int eventQueueSize_;

        DefaultEventDispatcherFactory(int eventQueueSize) {
            eventQueueSize_ = eventQueueSize;
        }

        @Override
        public DefaultEventDispatcher newEventDispatcher() {
            return new DefaultEventDispatcher(eventQueueSize_);
        }
    };

    /**
     * Constructs a instance.
     *
     * An invocation of this constructor behaves in exactly the same way as the invocation
     * <code>DefaultEventDispatcherGroup(workers, Executors.defaultThreadFactory(), 0)</code>.
     *
     * @param workers the number of the threads to be managed by the instance
     */
    public DefaultEventDispatcherGroup(int workers) {
        this(workers, Executors.defaultThreadFactory(), 0);
    }

    /**
     * Constructs a instance.
     *
     * An invocation of this constructor behaves in exactly the same way as the invocation
     * <code>DefaultEventDispatcherGroup(workers, threadFactory, 0)</code>.
     * @param workers the number of the threads to be managed by the instance
     * @param threadFactory a factory to create thread which runs a event dispatcher
     */
    public DefaultEventDispatcherGroup(int workers, ThreadFactory threadFactory) {
        this(workers, threadFactory, 0);
    }

    /**
     * Constructs a instance.
     *
     * @param workers the number of the threads to be managed by the instance
     * @param threadFactory a factory to create thread which runs a event dispatcher
     * @param eventQueueCapacity the capacity of the event queue used by {@code DefaultEventDispatcher},
     *                           zero or negative if the queue has no limit
     */
    public DefaultEventDispatcherGroup(int workers, ThreadFactory threadFactory, int eventQueueCapacity) {
        super(workers, threadFactory, new DefaultEventDispatcherFactory(eventQueueCapacity));
    }
}
