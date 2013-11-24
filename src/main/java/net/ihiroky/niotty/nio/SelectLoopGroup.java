package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.TaskLoopGroup;
import net.ihiroky.niotty.util.Arguments;

import java.util.concurrent.ThreadFactory;

/**
 *
 */
public class SelectLoopGroup extends TaskLoopGroup<SelectLoop> {

    private int readBufferSize_;
    private int writeBufferSize_;
    private boolean useDirectBuffer_;

    static final int DEFAULT_READ_BUFFER_SIZE = 8192;
    static final int DEFAULT_WRITE_BUFFER_SIZE = 8192;
    static final boolean DEFAULT_USE_DIRECT_BUFFER = false;

    /**
     * Constructs a new instance.
     *
     * @param threadFactory a factory to create thread which runs a task loop
     * @param workers       the number of threads held in the thread pool
     */
    public SelectLoopGroup(ThreadFactory threadFactory, int workers) {
        super(threadFactory, workers);
        readBufferSize_ = DEFAULT_READ_BUFFER_SIZE;
        writeBufferSize_ = DEFAULT_WRITE_BUFFER_SIZE;
        useDirectBuffer_ = DEFAULT_USE_DIRECT_BUFFER;
    }

    public static SelectLoopGroup newNonIoInstance(String threadNamePrefix) {
        return new SelectLoopGroup(new NameCountThreadFactory(threadNamePrefix), 1)
                .setReadBufferSize(0).setWriteBufferSize(0);
    }

    public SelectLoopGroup setReadBufferSize(int readBufferSize) {
        readBufferSize_ = Arguments.requirePositiveOrZero(readBufferSize, "readBufferSize");
        return this;
    }

    public SelectLoopGroup setWriteBufferSize(int writeBufferSize) {
        writeBufferSize_ = Arguments.requirePositiveOrZero(writeBufferSize, "writeBufferSize");
        return this;
    }

    public SelectLoopGroup setUseDirectBuffer(boolean useDirectBuffer) {
        useDirectBuffer_ = useDirectBuffer;
        return this;
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
    protected SelectLoop newTaskLoop() {
        return new SelectLoop(readBufferSize_, writeBufferSize_, useDirectBuffer_);
    }
}
