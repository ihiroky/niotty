package net.ihiroky.niotty;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * <p>An implementation of {@link net.ihiroky.niotty.EventDispatcherGroup} that manages
 * {@link net.ihiroky.niotty.SynchronizedEventDispatcher}.</p>
 *
 * <p>This class use threads to execute the {@link net.ihiroky.niotty.SynchronizedEventDispatcher}. </p>
 */
public final class SynchronizedEventDispatcherGroup
        extends EventDispatcherGroup {

    private static class SynchronizedEventDispatcherFactory
            implements EventDispatcherFactory<SynchronizedEventDispatcher> {

        private int eventQueueSize_;

        SynchronizedEventDispatcherFactory(int eventQueueSize) {
            eventQueueSize_ = eventQueueSize;
        }

        @Override
        public SynchronizedEventDispatcher newEventDispatcher() {
            return new SynchronizedEventDispatcher(eventQueueSize_);
        }
    };

    /**
     * Constructs a instance.
     *
     * An invocation of this constructor behaves in exactly the same way as the invocation
     * {@code SynchronizedEventDispatcherGroup(numberOfThread, Executors.defaultThreadFactory(), 0)}.
     *
     * @param workers the number of the threads to be managed by the instance
     */
    public SynchronizedEventDispatcherGroup(int workers) {
        this(workers, Executors.defaultThreadFactory(), 0);
    }

    /**
     * Constructs a instance.
     *
     * @param workers the number of the threads to be managed by the instance
     * @param threadFactory a factory to create thread which runs a event dispatcher
     */
    public SynchronizedEventDispatcherGroup(int workers, ThreadFactory threadFactory, int eventQueueSize) {
        super(workers, threadFactory, new SynchronizedEventDispatcherFactory(eventQueueSize));
    }
}
