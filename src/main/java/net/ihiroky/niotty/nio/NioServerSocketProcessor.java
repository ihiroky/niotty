package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.NameCountThreadFactory;

/**
 * Created on 13/01/10, 14:37
 *
 * @author Hiroki Itoh
 */
public class NioServerSocketProcessor extends AbstractProcessor<NioServerSocketTransport, NioServerSocketConfig> {

    private AcceptSelectorPool acceptSelectorPool_;
    private TcpIOSelectorPool ioSelectorPool_;
    private int readBufferSize_;
    private int writeBufferSize_;
    private boolean useDirectBuffer_;

    private int numberOfAcceptThread_;
    private int numberOfMessageIOThread_;

    private static final int DEFAULT_NUMBER_OF_ACCEPT_THREAD = 1;
    private static final int DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD =
            Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final boolean DEFAULT_DIRECT_BUFFER = true;

    static final String DEFAULT_NAME = "NioServerSocket";

    public NioServerSocketProcessor() {
        acceptSelectorPool_ = new AcceptSelectorPool();
        ioSelectorPool_ = new TcpIOSelectorPool();

        numberOfAcceptThread_ = DEFAULT_NUMBER_OF_ACCEPT_THREAD;
        numberOfMessageIOThread_ = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        readBufferSize_ = DEFAULT_BUFFER_SIZE;
        writeBufferSize_ = DEFAULT_BUFFER_SIZE;
        useDirectBuffer_ = DEFAULT_DIRECT_BUFFER;

        setName(DEFAULT_NAME);
    }

    @Override
    protected void onStart() {
        ioSelectorPool_.setReadBufferSize(readBufferSize_);
        ioSelectorPool_.setDirect(useDirectBuffer_);
        ioSelectorPool_.open(new NameCountThreadFactory(name().concat("-MessageIO")), numberOfMessageIOThread_);
        acceptSelectorPool_.open(new NameCountThreadFactory(name().concat("-Accept")), numberOfAcceptThread_);
    }

    @Override
    protected void onStop() {
        acceptSelectorPool_.close();
        ioSelectorPool_.close();
    }

    @Override
    public NioServerSocketTransport createTransport(NioServerSocketConfig config) {
        return new NioServerSocketTransport(config, this);
    }

    public void setNumberOfAcceptThread(int numberOfAcceptThread) {
        if (numberOfAcceptThread <= 0) {
            throw new IllegalArgumentException("numberOfAcceptThread must be positive.");
        }
        this.numberOfAcceptThread_ = numberOfAcceptThread;
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

    AcceptSelectorPool getAcceptSelectorPool() {
        return acceptSelectorPool_;
    }

    AbstractSelectorPool<TcpIOSelector> getMessageIOSelectorPool() {
        return ioSelectorPool_;
    }

    int getReadBufferSize() {
        return readBufferSize_;
    }

    int getWriteBufferSize() {
        return writeBufferSize_;
    }

    boolean isUseDirectBuffer() {
        return useDirectBuffer_;
    }
}
