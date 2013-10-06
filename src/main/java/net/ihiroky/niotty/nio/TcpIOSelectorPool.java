package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TaskLoopGroup;
import net.ihiroky.niotty.util.Arguments;

import java.util.concurrent.ThreadFactory;

/**
 * An implementation of {@link net.ihiroky.niotty.nio.AcceptSelectorPool}
 * to handle {@link net.ihiroky.niotty.nio.TcpIOSelector}.
 */
public class TcpIOSelectorPool extends TaskLoopGroup<TcpIOSelector> {

    private final int readBufferSize_;
    private final boolean useDirectBuffer_;
    private final boolean duplicateReadBuffer_;

    static final int DEFAULT_READ_BUFFER_SIZE = 8192;
    static final boolean DEFAULT_USE_DIRECT_BUFFER = false;
    static final boolean DEFAULT_DUPLICATE_READ_BUFFER = true;

    /**
     * Creates a new instance.
     *
     * @param threadFactory a factory to create thread which runs this object
     * @param workers the number of threads
     */
    public TcpIOSelectorPool(ThreadFactory threadFactory, int workers) {
        this(threadFactory, workers, DEFAULT_READ_BUFFER_SIZE, DEFAULT_USE_DIRECT_BUFFER, DEFAULT_DUPLICATE_READ_BUFFER);
    }

    /**
     * Creates a new instance.
     *
     * @param threadFactory a factory to create thread which runs this object
     * @param workers the number of threads
     * @param readBufferSize the size of a read buffer used to read data from a channel
     * @param useDirectBuffer true if the read buffer is direct
     * @param duplicateReadBuffer true if a content of the read buffer is duplicated
     *                            when the content is passed to a pipeline
     */
    public TcpIOSelectorPool(ThreadFactory threadFactory, int workers,
            int readBufferSize, boolean useDirectBuffer, boolean duplicateReadBuffer) {
        super(threadFactory, workers);
        readBufferSize_ = Arguments.requirePositive(readBufferSize, "readBufferSize");
        useDirectBuffer_ = useDirectBuffer;
        duplicateReadBuffer_ = duplicateReadBuffer;
    }

    public int readBufferSize() {
        return readBufferSize_;
    }

    public boolean useDirectBuffer() {
        return useDirectBuffer_;
    }

    public boolean duplicateReadBuffer() {
        return duplicateReadBuffer_;
    }

    @Override
    protected TcpIOSelector newTaskLoop() {
        return new TcpIOSelector(readBufferSize_, useDirectBuffer_, duplicateReadBuffer_);
    }
}
