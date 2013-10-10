package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.util.Arguments;

/**
 * An implementation of {@link net.ihiroky.niotty.Processor} for NIO {@code SocketChannel}.
 */
public class NioClientSocketProcessor extends AbstractProcessor<NioClientSocketTransport> {

    private ConnectSelectorPool connectSelectorPool_;
    private TcpIOSelectorPool ioSelectorPool_;
    private int numberOfConnectThread_;
    private int numberOfMessageIOThread_;
    private WriteQueueFactory writeQueueFactory_;

    private int readBufferSize_;
    private boolean useDirectBuffer_;
    private boolean duplicateReadBuffer_;

    private static final int DEFAULT_NUMBER_OF_CONNECT_THREAD = 1;
    private static final int DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD = 1;

    static final String DEFAULT_NAME = "NioClientSocket";

    public NioClientSocketProcessor() {
        writeQueueFactory_ = new SimpleWriteQueueFactory();
        numberOfConnectThread_ = DEFAULT_NUMBER_OF_CONNECT_THREAD;
        numberOfMessageIOThread_ = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        readBufferSize_ = TcpIOSelectorPool.DEFAULT_READ_BUFFER_SIZE;
        useDirectBuffer_ = TcpIOSelectorPool.DEFAULT_USE_DIRECT_BUFFER;
        duplicateReadBuffer_ = TcpIOSelectorPool.DEFAULT_DUPLICATE_READ_BUFFER;

        setName(DEFAULT_NAME);
    }

    @Override
    public NioClientSocketTransport createTransport() {
        return new NioClientSocketTransport(pipelineComposer(), name(),
                connectSelectorPool_, ioSelectorPool_, writeQueueFactory_);
    }

    @Override
    protected void onStart() {
        ioSelectorPool_ = new TcpIOSelectorPool(
                new NameCountThreadFactory(name().concat("-IO")), numberOfMessageIOThread_,
                readBufferSize_, useDirectBuffer_, duplicateReadBuffer_);

        if (numberOfConnectThread_ > 0) {
            connectSelectorPool_ = new ConnectSelectorPool(
                    new NameCountThreadFactory(name().concat("-Connect")), numberOfConnectThread_);
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

    public NioClientSocketProcessor setNumberOfConnectThread(int numberOfConnectThread) {
        numberOfConnectThread_ = Arguments.requirePositiveOrZero(numberOfConnectThread, "numberOfConnectThread");
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
        duplicateReadBuffer_ = duplicateReadBuffer;
        return this;
    }

    public NioClientSocketProcessor setWriteQueueFactory(WriteQueueFactory writeQueueFactory) {
        writeQueueFactory_ = Arguments.requireNonNull(writeQueueFactory, "writeQueueFactory");
        return this;
    }

    boolean hasConnectSelector() {
        return numberOfConnectThread_ > 0;
    }

    ConnectSelectorPool connectSelectorPool() {
        return connectSelectorPool_;
    }

    TcpIOSelectorPool ioSelectorPool() {
        return ioSelectorPool_;
    }

    public int numberOfConnectThread() {
        return numberOfConnectThread_;
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
        return duplicateReadBuffer_;
    }
}
