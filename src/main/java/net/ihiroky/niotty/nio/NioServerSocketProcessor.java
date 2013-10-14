package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.util.Arguments;

/**
 * An implementation of {@link net.ihiroky.niotty.Processor} for NIO {@code ServerSocketChannel}.
 */
public class NioServerSocketProcessor extends AbstractProcessor<NioServerSocketTransport> {

    private SelectLoopGroup acceptSelectLoopGroup_;
    private SelectLoopGroup ioSelectLoopGroup_;
    private WriteQueueFactory writeQueueFactory_;

    private int numberOfIoThread_;
    private int readBufferSize_;
    private boolean useDirectBuffer_;
    private boolean copyReadBuffer_;

    private static final int DEFAULT_NUMBER_OF_IO_THREAD =
            Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);

    static final String DEFAULT_NAME = "NioServerSocket";

    public NioServerSocketProcessor() {
        writeQueueFactory_ = new SimpleWriteQueueFactory();

        numberOfIoThread_ = DEFAULT_NUMBER_OF_IO_THREAD;
        readBufferSize_ = SelectLoopGroup.DEFAULT_READ_BUFFER_SIZE;
        useDirectBuffer_ = SelectLoopGroup.DEFAULT_USE_DIRECT_BUFFER;
        copyReadBuffer_ = SelectLoopGroup.DEFAULT_COPY_READ_BUFFER;

        setName(DEFAULT_NAME);
    }

    @Override
    protected void onStart() {
        ioSelectLoopGroup_ = new SelectLoopGroup(
                new NameCountThreadFactory(name().concat("-MessageIO")), numberOfIoThread_)
                .setReadBufferSize(readBufferSize_)
                .setWriteBufferSize(0)
                .setUseDirectBuffer(useDirectBuffer_)
                .setCopyReadBuffer(copyReadBuffer_);
        acceptSelectLoopGroup_ = SelectLoopGroup.newNonIoInstance(name().concat("-Accept"));
    }

    @Override
    protected void onStop() {
        acceptSelectLoopGroup_.close();
        ioSelectLoopGroup_.close();
    }

    @Override
    public NioServerSocketTransport createTransport() {
        return new NioServerSocketTransport(name(), pipelineComposer(),
                acceptSelectLoopGroup_, ioSelectLoopGroup_, writeQueueFactory_);
    }

    @Override
    public NioServerSocketProcessor setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public NioServerSocketProcessor setPipelineComposer(PipelineComposer composer) {
        super.setPipelineComposer(composer);
        return this;
    }


    public NioServerSocketProcessor setNumberOfIoThread(int numberOfIoThread) {
        this.numberOfIoThread_ = Arguments.requirePositive(numberOfIoThread, "numberOfIoThread");
        return this;
    }

    public NioServerSocketProcessor setReadBufferSize(int readBufferSize) {
        readBufferSize_ = Arguments.requirePositive(readBufferSize, "readBufferSize");
        return this;
    }

    public NioServerSocketProcessor setUseDirectBuffer(boolean useDirectBuffer) {
        useDirectBuffer_ = useDirectBuffer;
        return this;
    }

    public NioServerSocketProcessor setDuplicateReadBuffer(boolean duplicateReadBuffer) {
        copyReadBuffer_ = duplicateReadBuffer;
        return this;
    }

    public NioServerSocketProcessor setWriteQueueFactory(WriteQueueFactory writeQueueFactory) {
        writeQueueFactory_ = writeQueueFactory;
        return this;
    }

    SelectLoopGroup ioSelectLoopGroup_() {
        return ioSelectLoopGroup_;
    }

    WriteQueueFactory writeQueueFactory() {
        return writeQueueFactory_;
    }

    public int numberOfMessageIOThread() {
        return numberOfIoThread_;
    }

    public int readBufferSize() {
        return readBufferSize_;
    }

    public boolean useDirectBuffer() {
        return useDirectBuffer_;
    }

    public boolean duplicateReadBuffer() {
        return copyReadBuffer_;
    }
}
