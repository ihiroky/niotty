package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.util.Arguments;

/**
 * An implementation of {@link net.ihiroky.niotty.Processor} for NIO {@code SocketChannel}.
 */
public class NioClientSocketProcessor extends AbstractProcessor<NioClientSocketTransport> {

    private SelectLoopGroup connectSelectorPool_;
    private SelectLoopGroup ioSelectorPool_;
    private int numberOfMessageIOThread_;
    private WriteQueueFactory writeQueueFactory_;
    private boolean useNonBlockingConnection_;

    private int readBufferSize_;
    private boolean useDirectBuffer_;
    private boolean copyReadBuffer_;

    private static final int DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD = 1;

    static final String DEFAULT_NAME = "NioClientSocket";

    public NioClientSocketProcessor() {
        writeQueueFactory_ = new SimpleWriteQueueFactory();
        numberOfMessageIOThread_ = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        readBufferSize_ = SelectLoopGroup.DEFAULT_READ_BUFFER_SIZE;
        useDirectBuffer_ = SelectLoopGroup.DEFAULT_USE_DIRECT_BUFFER;
        copyReadBuffer_ = SelectLoopGroup.DEFAULT_COPY_READ_BUFFER;

        setName(DEFAULT_NAME);
    }

    @Override
    public NioClientSocketTransport createTransport() {
        return new NioClientSocketTransport(name(),pipelineComposer(),
                connectSelectorPool_, ioSelectorPool_, writeQueueFactory_);
    }

    @Override
    protected void onStart() {
        ioSelectorPool_ = new SelectLoopGroup(
                new NameCountThreadFactory(name().concat("-IO")), numberOfMessageIOThread_)
                .setReadBufferSize(readBufferSize_)
                .setWriteBufferSize(0)
                .setUseDirectBuffer(useDirectBuffer_)
                .setCopyReadBuffer(copyReadBuffer_);

        if (useNonBlockingConnection_) {
            connectSelectorPool_ = SelectLoopGroup.newNonIoInstance(name().concat("-Connect"));
        }
    }

    @Override
    protected void onStop() {
        ioSelectorPool_.close();
        if (connectSelectorPool_ != null) {
            connectSelectorPool_.close();
        }
    }

    @Override
    public NioClientSocketProcessor setPipelineComposer(PipelineComposer composer) {
        super.setPipelineComposer(composer);
        return this;
    }

    @Override
    public NioClientSocketProcessor setName(String name) {
        super.setName(name);
        return this;
    }

    public NioClientSocketProcessor setUseNonBlockingConnection(boolean useNonBlockingConnection) {
        useNonBlockingConnection_ = useNonBlockingConnection;
        return this;
    }

    public NioClientSocketProcessor setNumberOfMessageIOThread(int numberOfMessageIOThread) {
        numberOfMessageIOThread_ = Arguments.requirePositive(numberOfMessageIOThread, "numberOfMessageIOThread");
        return this;
    }

    public NioClientSocketProcessor setReadBufferSize(int readBufferSize) {
        readBufferSize_ = Arguments.requirePositive(readBufferSize, "readBufferSize");
        return this;
    }

    public NioClientSocketProcessor setUseDirectBuffer(boolean useDirectBuffer) {
        useDirectBuffer_ = useDirectBuffer;
        return this;
    }

    public NioClientSocketProcessor setDuplicateReadBuffer(boolean duplicateReadBuffer) {
        copyReadBuffer_ = duplicateReadBuffer;
        return this;
    }

    public NioClientSocketProcessor setWriteQueueFactory(WriteQueueFactory writeQueueFactory) {
        writeQueueFactory_ = Arguments.requireNonNull(writeQueueFactory, "writeQueueFactory");
        return this;
    }

    boolean useNonBlockingConnection() {
        return useNonBlockingConnection_;
    }

    public int numberOfMessageIOThread() {
        return numberOfMessageIOThread_;
    }

    public int readBufferSize() {
        return readBufferSize_;
    }

    public boolean useDirectBuffer() {
        return useDirectBuffer_;
    }

    public boolean duplicateReceiveBuffer() {
        return copyReadBuffer_;
    }
}
