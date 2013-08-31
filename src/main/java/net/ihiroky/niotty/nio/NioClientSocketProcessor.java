package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.PipelineComposer;

import java.util.Objects;

/**
 * An implementation of {@link net.ihiroky.niotty.Processor} for NIO {@code SocketChannel}.
 */
public class NioClientSocketProcessor extends AbstractProcessor<NioClientSocketTransport> {

    private ConnectSelectorPool connectSelectorPool_;
    private TcpIOSelectorPool ioSelectorPool_;
    private int numberOfConnectThread_;
    private int numberOfMessageIOThread_;
    private WriteQueueFactory writeQueueFactory_;

    private static final int DEFAULT_NUMBER_OF_CONNECT_THREAD = 1;
    private static final int DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD = 1;

    static final String DEFAULT_NAME = "NioClientSocket";

    public NioClientSocketProcessor() {
        ioSelectorPool_ = new TcpIOSelectorPool();
        connectSelectorPool_ = new ConnectSelectorPool();
        writeQueueFactory_ = new SimpleWriteQueueFactory();

        numberOfConnectThread_ = DEFAULT_NUMBER_OF_CONNECT_THREAD;
        numberOfMessageIOThread_ = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        setName(DEFAULT_NAME);
    }

    @Override
    public NioClientSocketTransport createTransport() {
        return new NioClientSocketTransport(pipelineComposer(), NioClientSocketTransport.DEFAULT_WEIGHT, name(),
                connectSelectorPool_, ioSelectorPool_, writeQueueFactory_);
    }

    /**
     * Constructs the transport.
     *
     * @param weight a weight to choose I/O thread.
     * @return the transport.
     */
    public NioClientSocketTransport createTransport(int weight) {
        return new NioClientSocketTransport(pipelineComposer(), weight, name(),
                connectSelectorPool_, ioSelectorPool_, writeQueueFactory_);
    }

    @Override
    protected void onStart() {
        ioSelectorPool_.open(new NameCountThreadFactory(name().concat("-IO")), numberOfMessageIOThread_);

        if (numberOfConnectThread_ > 0) {
            connectSelectorPool_.open(new NameCountThreadFactory(name().concat("-Connect")), numberOfConnectThread_);
        }
    }

    @Override
    protected void onStop() {
        ioSelectorPool_.close();
        connectSelectorPool_.close();
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
        if (numberOfConnectThread < 0) {
            throw new IllegalArgumentException("numberOfConnectThread must be positive or zero.");
        }
        // TODO Set null if numberOfConnectThread is zero and create instance if it changes from zero to positive.
        this.numberOfConnectThread_ = numberOfConnectThread;
        return this;
    }

    public NioClientSocketProcessor setNumberOfMessageIOThread(int numberOfMessageIOThread) {
        if (numberOfMessageIOThread <= 0) {
            throw new IllegalArgumentException("numberOfMessageIOThread must be positive.");
        }
        this.numberOfMessageIOThread_ = numberOfMessageIOThread;
        return this;
    }

    public NioClientSocketProcessor setReadBufferSize(int readBufferSize) {
        if (readBufferSize <= 0) {
            throw new IllegalArgumentException("readBufferSize must be positive.");
        }
        ioSelectorPool_.setReadBufferSize(readBufferSize);
        return this;
    }

    public NioClientSocketProcessor setUseDirectBuffer(boolean useDirectBuffer) {
        ioSelectorPool_.setDirect(useDirectBuffer);
        return this;
    }

    public NioClientSocketProcessor setDuplicateReceiveBuffer(boolean duplicateReceiveBuffer) {
        ioSelectorPool_.setDuplicateBuffer(duplicateReceiveBuffer);
        return this;
    }

    public NioClientSocketProcessor setTaskWeightThresholdOfIOSelectorPool(int threshold) {
        ioSelectorPool_.setTaskWeightThreshold(threshold);
        return this;
    }

    public NioClientSocketProcessor setWriteQueueFactory(WriteQueueFactory writeQueueFactory) {
        Objects.requireNonNull(writeQueueFactory, "writeQueueFactory");
        writeQueueFactory_ = writeQueueFactory;
        return this;
    }

    boolean hasConnectSelector() {
        return numberOfConnectThread_ > 0;
    }

    ConnectSelectorPool connectSelectorPool() {
        return connectSelectorPool_;
    }

    AbstractSelectorPool<TcpIOSelector> ioSelectorPool() {
        return ioSelectorPool_;
    }

    public int numberOfConnectThread() {
        return numberOfConnectThread_;
    }

    public int numberOfMessageIOThread() {
        return numberOfMessageIOThread_;
    }

    public int readBufferSize() {
        return ioSelectorPool_.readBufferSize();
    }

    public boolean isUseDirectBuffer() {
        return ioSelectorPool_.direct();
    }

    public boolean isDuplicateReceiveBuffer() {
        return ioSelectorPool_.duplicateBuffer();
    }

    public int taskWeightThresholdOfIOSelectorPool() {
        return ioSelectorPool_.getTaskWeightThreshold();
    }
}
