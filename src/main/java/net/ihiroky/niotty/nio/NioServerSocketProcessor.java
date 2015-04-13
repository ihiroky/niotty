package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.util.Arguments;

/**
 * An implementation of {@link net.ihiroky.niotty.Processor} for NIO {@code ServerSocketChannel}.
 */
public class NioServerSocketProcessor extends AbstractProcessor<NioServerSocketTransport> {

    private NioEventDispatcherGroup acceptSelectDispatcherGroup_;
    private NioEventDispatcherGroup ioSelectDispatcherGroup_;
    private WriteQueueFactory<PacketQueue> writeQueueFactory_;

    private int numberOfIoThread_;
    private int readBufferSize_;
    private boolean useDirectBuffer_;
    private boolean copyReadBuffer_;

    private static final int DEFAULT_NUMBER_OF_IO_THREAD =
            Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);

    static final String DEFAULT_NAME = "NioServerSocket";

    public NioServerSocketProcessor() {
        writeQueueFactory_ = new SimplePacketQueueFactory();

        numberOfIoThread_ = DEFAULT_NUMBER_OF_IO_THREAD;
        readBufferSize_ = SelectDispatcherFactory.DEFAULT_READ_BUFFER_SIZE;
        useDirectBuffer_ = SelectDispatcherFactory.DEFAULT_USE_DIRECT_BUFFER;

        setName(DEFAULT_NAME);
    }

    @Override
    protected void onStart() {
        ioSelectDispatcherGroup_ = NioEventDispatcherGroup.newBuilder()
                .setWorkers(numberOfIoThread_)
                .setThreadFactory(new NameCountThreadFactory(name().concat("-IO")))
                .setReadBufferSize(readBufferSize_)
                .setWriteBufferSize(0)
                .setUseDirectBuffer(useDirectBuffer_)
                .build();
        acceptSelectDispatcherGroup_ = NioEventDispatcherGroup.newBuilder()
                .setWorkers(1)
                .setThreadFactory(new NameCountThreadFactory(name().concat("-Accept")))
                .setBufferSizeNonIo()
                .build();
    }

    @Override
    protected void onStop() {
        acceptSelectDispatcherGroup_.close();
        ioSelectDispatcherGroup_.close();
    }

    @Override
    public NioServerSocketTransport createTransport() {
        return new NioServerSocketTransport(name(), pipelineComposer(),
                acceptSelectDispatcherGroup_, ioSelectDispatcherGroup_, writeQueueFactory_);
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

    public NioServerSocketProcessor setWriteQueueFactory(WriteQueueFactory<PacketQueue> writeQueueFactory) {
        writeQueueFactory_ = writeQueueFactory;
        return this;
    }

    NioEventDispatcherGroup ioEventDispatcherGroup() {
        return ioSelectDispatcherGroup_;
    }

    WriteQueueFactory<PacketQueue> writeQueueFactory() {
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
