package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventDispatcher;
import net.ihiroky.niotty.EventDispatcherFactory;
import net.ihiroky.niotty.EventDispatcherGroup;
import net.ihiroky.niotty.util.Arguments;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * A {@link EventDispatcherGroup} for the NIO event dispatching.
 * This EventDispatcherGroup requires NIO parameters: read buffer size, write buffer size
 * and flag to use direct buffer.
 * @see EventDispatcherGroup
 */
public class NioEventDispatcherGroup extends EventDispatcherGroup {


    /**
     * Constructs a new instance.
     *
     * @param workers                the number of threads held in the thread pool
     * @param threadFactory          a factory to create thread which runs a event dispatcher
     * @param eventDispatcherFactory a factory to create the instance of {@link EventDispatcher}
     */
    NioEventDispatcherGroup(int workers, ThreadFactory threadFactory, EventDispatcherFactory eventDispatcherFactory) {
        super(workers, threadFactory, eventDispatcherFactory);
    }

    /**
     * Creates a new builder to build {@link NioEventDispatcherGroup}.
     * @return a new builder to build {@link NioEventDispatcherGroup}.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder to build {@link NioEventDispatcherGroup}.
     */
    public static class Builder {
        private int workers_;
        private ThreadFactory threadFactory_;
        private int eventQueueCapacity_;
        private int readBufferSize_;
        private int writeBufferSize_;
        private boolean useDirectBuffer_;

        static final int DEFAULT_READ_BUFFER_SIZE = 8192;
        static final int DEFAULT_WRITE_BUFFER_SIZE = 8192;
        static final boolean DEFAULT_USE_DIRECT_BUFFER = false;

        Builder() {
            workers_ = 1;
            threadFactory_ = Executors.defaultThreadFactory();
            eventQueueCapacity_ = 0;
            readBufferSize_ = DEFAULT_READ_BUFFER_SIZE;
            writeBufferSize_ = DEFAULT_WRITE_BUFFER_SIZE;
            useDirectBuffer_ = DEFAULT_USE_DIRECT_BUFFER;
        }

        /**
         * Sets the number of workers.
         * @param workers the number of workers
         * @return this builder
         */
        public Builder setWorkers(int workers) {
            workers_ = Arguments.requirePositive(workers, "workers");
            return this;
        }

        /**
         * Sets the thread factory.
         * @param threadFactory the thread factroy
         * @return this builder
         */
        public Builder setThreadFactory(ThreadFactory threadFactory) {
            threadFactory_ = (threadFactory != null) ? threadFactory : Executors.defaultThreadFactory();
            return this;
        }

        /**
         * Sets the read and write buffer size for the non IO (accept) selector.
         * An invocation of this method behaves in the same way as the invocation
         * {@code setReadBufferSize(0).setWriteBufferSize(0)}.
         * @return this builder
         */
        public Builder setBufferSizeNonIo() {
            readBufferSize_ = 0;
            writeBufferSize_ = 0;
            return this;
        }

        /**
         * Sets the size of the event queue.
         * @param eventQueueCapacity the size of the event queue
         * @return this builder
         */
        public Builder setEventQueueCapacity(int eventQueueCapacity) {
            eventQueueCapacity_ = eventQueueCapacity;
            return this;
        }

        /**
         * Sets the read buffer size.
         * @param readBufferSize the read buffer size
         * @return this builder
         */
        public Builder setReadBufferSize(int readBufferSize) {
            readBufferSize_ = Arguments.requirePositiveOrZero(readBufferSize, "readBufferSize");
            return this;
        }

        /**
         * Sets the write buffer size.
         * @param writeBufferSize the write buffer size
         * @return this builder
         */
        public Builder setWriteBufferSize(int writeBufferSize) {
            writeBufferSize_ = Arguments.requirePositiveOrZero(writeBufferSize, "writeBufferSize");
            return this;
        }

        /**
         * Sets true if use the direct buffer.
         * @param useDirectBuffer true if use the direct buffer
         * @return this builder
         */
        public Builder setUseDirectBuffer(boolean useDirectBuffer) {
            useDirectBuffer_ = useDirectBuffer;
            return this;
        }

        /**
         * Returns the number of workers.
         * @return the number of workers
         */
        public int workers() {
            return workers_;
        }

        /**
         * Return the thread factory.
         * @return the thread factory
         */
        public ThreadFactory threadFactory() {
            return threadFactory_;
        }

        /**
         * Returns the size of the event queue.
         * @return the size of the event queue
         */
        public int eventQueueCapacity() {
            return eventQueueCapacity_;
        }

        /**
         * Returns the read buffer size.
         * @return the read buffer size
         */
        public int readBufferSize_() {
            return readBufferSize_;
        }

        /**
         * Returns the write buffer size.
         * @return the write buffer size
         */
        public int writeBufferSize_() {
            return writeBufferSize_;
        }

        /**
         * Returns true if use the direct buffer
         * @return true if use the direct buffer
         */
        public boolean useDirectBuffer() {
            return useDirectBuffer_;
        }

        /**
         * Builds the new {@link NioEventDispatcherGroup}.
         * @return
         */
        public NioEventDispatcherGroup build() {
            return new NioEventDispatcherGroup(workers_, threadFactory(), new EventDispatcherFactory() {
                @Override
                public EventDispatcher newEventDispatcher() {
                    return new NioEventDispatcher(
                            eventQueueCapacity_, readBufferSize_, writeBufferSize_, useDirectBuffer_);
                }
            });
        }

    }
}
