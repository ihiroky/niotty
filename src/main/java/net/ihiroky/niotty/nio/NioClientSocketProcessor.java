package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.NameCountThreadFactory;

/**
 * Created on 13/01/18, 12:38
 *
 * @author Hiroki Itoh
 */
public class NioClientSocketProcessor extends AbstractProcessor<NioClientSocketTransport, NioClientSocketConfig> {

    private ConnectSelectorPool connectSelectorPool_;
    private TcpIOSelectorPool ioSelectorPool_;
    private int numberOfConnectThread_;
    private int numberOfMessageIOThread_;
    private int readBufferSize_;
    private int writeBufferSize_;
    private boolean useDirectBuffer_;

    private static final int DEFAULT_NUMBER_OF_CONNECT_THREAD = 1;
    private static final int DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD =
            Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final boolean DEFAULT_DIRECT_BUFFER = true;

    static final String DEFAULT_NAME = "NioClientSocket";

    public NioClientSocketProcessor() {
        ioSelectorPool_ = new TcpIOSelectorPool();
        connectSelectorPool_ = new ConnectSelectorPool(ioSelectorPool_);

        numberOfConnectThread_ = DEFAULT_NUMBER_OF_CONNECT_THREAD;
        numberOfMessageIOThread_ = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        readBufferSize_ = DEFAULT_BUFFER_SIZE;
        writeBufferSize_ = DEFAULT_BUFFER_SIZE;
        useDirectBuffer_ = DEFAULT_DIRECT_BUFFER;
        setName(DEFAULT_NAME);
    }

    @Override
    public NioClientSocketTransport createTransport(NioClientSocketConfig config) {
        return new NioClientSocketTransport(config, pipelineComposer(),
                NioClientSocketTransport.DEFAULT_WEIGHT, name(), connectSelectorPool_);
    }

    /**
     * Constructs the transport.
     *
     * @param config a configuration to construct the transport.
     * @param weight a weight to choose I/O thread.
     * @return the transport.
     */
    public NioClientSocketTransport createTransport(NioClientSocketConfig config, int weight) {
        return new NioClientSocketTransport(config, pipelineComposer(), weight, name(), connectSelectorPool_);
    }

    @Override
    protected void onStart() {
        ioSelectorPool_.setReadBufferSize(readBufferSize_);
        ioSelectorPool_.setDirect(useDirectBuffer_);
        ioSelectorPool_.open(new NameCountThreadFactory(name().concat("-IO")), numberOfMessageIOThread_);
        connectSelectorPool_.open(new NameCountThreadFactory(name().concat("-Connect")), numberOfConnectThread_);
    }

    @Override
    protected void onStop() {
        ioSelectorPool_.close();
        connectSelectorPool_.close();
    }

    public void setNumberOfConnectThread(int numberOfConnectThread) {
        if (numberOfConnectThread <= 0) {
            throw new IllegalArgumentException("numberOfConnectThread must be positive.");
        }
        this.numberOfConnectThread_ = numberOfConnectThread;
    }

    public void setNumberOfMessageIOThread(int numberOfMessageIOThread) {
        if (numberOfMessageIOThread <= 0) {
            throw new IllegalArgumentException("numberOfMessageIOThread must be positive.");
        }
        this.numberOfMessageIOThread_ = numberOfMessageIOThread;
    }

    public void setReadBufferSize(int readBufferSize) {
        if (readBufferSize <= 0) {
            throw new IllegalArgumentException("readBufferSize must be positive.");
        }
        this.readBufferSize_ = readBufferSize;
    }

    public void setWriteBufferSize(int writeBufferSize) {
        if (writeBufferSize <= 0) {
            throw new IllegalArgumentException("writeBufferSize must be positive.");
        }
        this.writeBufferSize_ = writeBufferSize;
    }

    public void setUseDirectBuffer(boolean useDirectBuffer) {
        this.useDirectBuffer_ = useDirectBuffer;
    }

    public void setTaskWeightThresholdOfIOSelectorPool(int threshold) {
        ioSelectorPool_.setTaskWeightThreshold(threshold);
    }

    ConnectSelectorPool getConnectSelectorPool() {
        return connectSelectorPool_;
    }

    AbstractSelectorPool<TcpIOSelector> getMessageIOSelectorPool() {
        return ioSelectorPool_;
    }

    public int getReadBufferSize() {
        return readBufferSize_;
    }

    public int getWriteBufferSize() {
        return writeBufferSize_;
    }

    public boolean isUseDirectBuffer() {
        return useDirectBuffer_;
    }

    public int getTaskWeightThresholdOfIOSelectorPool() {
        return ioSelectorPool_.getTaskWeightThreshold();
    }
}
