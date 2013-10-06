package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TaskLoopGroup;
import net.ihiroky.niotty.util.Arguments;

import java.util.concurrent.ThreadFactory;

/**
 * @author Hiroki Itoh
 */
public class UdpIOSelectorPool extends TaskLoopGroup<UdpIOSelector> {

    private final int readBufferSize_;
    private final int writeBufferSize_;
    private final boolean useDirectBuffer_;
    private final boolean duplicateReadBuffer_;

    static final int DEFAULT_READ_BUFFER_SIZE = Short.MAX_VALUE;
    static final int DEFAULT_WRITE_BUFFER_SIZE = Short.MAX_VALUE;
    static final boolean DEFAULT_USE_DIRECT_BUFFER = false;
    static final boolean DEFAULT_DUPLICATE_READ_BUFFER = true;

    /**
     * Creates a new instance.
     *
     * @param threadFactory a factory to create thread which runs this object
     * @param workers the number of threads
     */
    public UdpIOSelectorPool(ThreadFactory threadFactory, int workers) {
        this(threadFactory, workers,
                DEFAULT_READ_BUFFER_SIZE, DEFAULT_WRITE_BUFFER_SIZE,
                DEFAULT_USE_DIRECT_BUFFER, DEFAULT_DUPLICATE_READ_BUFFER);
    }

    /**
     * Creates a new instance.
     *
     * @param threadFactory a factory to create thread which runs this object
     * @param workers the number of threads
     * @param readBufferSize the size of a read buffer used to read data from a channel
     * @param writeBufferSize the size of a write buffer used to read data from a non-connected channel
     * @param useDirectBuffer true if the buffers is direct.
     * @param duplicateReadBuffer true if a content of the read buffer is duplicated
     *                            when the content is passed to a pipeline
     */
    public UdpIOSelectorPool(ThreadFactory threadFactory, int workers,
            int readBufferSize, int writeBufferSize, boolean useDirectBuffer, boolean duplicateReadBuffer) {
        super(threadFactory, workers);
        readBufferSize_ = Arguments.requirePositive(readBufferSize, "readBufferSize");
        writeBufferSize_ = Arguments.requirePositive(writeBufferSize, "writeBufferSize");
        useDirectBuffer_ = useDirectBuffer;
        duplicateReadBuffer_ = duplicateReadBuffer;
    }

    public int readBufferSize() {
        return readBufferSize_;
    }

    public int writeBufferSize() {
        return writeBufferSize_;
    }

    public boolean direct() {
        return useDirectBuffer_;
    }

    public boolean duplicateBuffer() {
        return duplicateReadBuffer_;
    }

    @Override
    protected UdpIOSelector newTaskLoop() {
        return new UdpIOSelector(readBufferSize_, writeBufferSize_, useDirectBuffer_, duplicateReadBuffer_);
    }
}
