package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventDispatcherFactory;
import net.ihiroky.niotty.util.Arguments;

/**
 *
 */
public class SelectDispatcherFactory implements EventDispatcherFactory<NioEventDispatcher> {

    private int eventQueueCapacity_;
    private int readBufferSize_;
    private int writeBufferSize_;
    private boolean useDirectBuffer_;

    static final int DEFAULT_READ_BUFFER_SIZE = 8192;
    static final int DEFAULT_WRITE_BUFFER_SIZE = 8192;
    static final boolean DEFAULT_USE_DIRECT_BUFFER = false;

    public SelectDispatcherFactory() {
        eventQueueCapacity_ = 0;
        readBufferSize_ = DEFAULT_READ_BUFFER_SIZE;
        writeBufferSize_ = DEFAULT_WRITE_BUFFER_SIZE;
        useDirectBuffer_ = DEFAULT_USE_DIRECT_BUFFER;
    }

    static SelectDispatcherFactory newInstanceForNonIO() {
        return new SelectDispatcherFactory().setReadBufferSize(0).setWriteBufferSize(0);
    }

    public SelectDispatcherFactory setEventQueueCapacity(int eventQueueCapacity) {
        eventQueueCapacity_ = eventQueueCapacity;
        return this;
    }
    public SelectDispatcherFactory setReadBufferSize(int readBufferSize) {
        readBufferSize_ = Arguments.requirePositiveOrZero(readBufferSize, "readBufferSize");
        return this;
    }

    public SelectDispatcherFactory setWriteBufferSize(int writeBufferSize) {
        writeBufferSize_ = Arguments.requirePositiveOrZero(writeBufferSize, "writeBufferSize");
        return this;
    }

    public SelectDispatcherFactory setUseDirectBuffer(boolean useDirectBuffer) {
        useDirectBuffer_ = useDirectBuffer;
        return this;
    }

    public int eventQueueCapacity() {
        return eventQueueCapacity_;
    }

    public int readBufferSize_() {
        return readBufferSize_;
    }

    public int writeBufferSize_() {
        return writeBufferSize_;
    }

    public boolean useDirectBuffer() {
        return useDirectBuffer_;
    }

    @Override
    public NioEventDispatcher newEventDispatcher() {
        return new NioEventDispatcher(eventQueueCapacity_, readBufferSize_, writeBufferSize_, useDirectBuffer_);
    }
}
