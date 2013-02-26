package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.Processor;

import java.util.Objects;

/**
 * Created on 13/01/18, 12:38
 *
 * @author Hiroki Itoh
 */
public class NioClientSocketProcessor implements Processor<NioClientSocketTransport, NioClientSocketConfig> {

    private ConnectSelectorPool connectSelectorPool_;
    private MessageIOSelectorPool messageIOSelectorPool_;
    private String name_;
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

    static final String DEFAULT_NAME = "NioServerSocket";

    public NioClientSocketProcessor() {
        connectSelectorPool_ = new ConnectSelectorPool();
        messageIOSelectorPool_ = new MessageIOSelectorPool();
        name_ = "NioClientSocket";
        numberOfConnectThread_ = DEFAULT_NUMBER_OF_CONNECT_THREAD;
        numberOfMessageIOThread_ = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        readBufferSize_ = DEFAULT_BUFFER_SIZE;
        writeBufferSize_ = DEFAULT_BUFFER_SIZE;
        useDirectBuffer_ = DEFAULT_DIRECT_BUFFER;
    }

    @Override
    public NioClientSocketTransport createTransport(NioClientSocketConfig config) {
        return new NioClientSocketTransport(config, this);
    }

    @Override
    public void start() {
        messageIOSelectorPool_.setReadBufferSize(readBufferSize_);
        messageIOSelectorPool_.setDirect(useDirectBuffer_);
        messageIOSelectorPool_.open(new NameCountThreadFactory(name_.concat("-MessageIO")), numberOfMessageIOThread_);
        connectSelectorPool_.open(new NameCountThreadFactory(name_.concat("-Connect")), numberOfConnectThread_);
    }

    @Override
    public void stop() {
        messageIOSelectorPool_.close();
        connectSelectorPool_.close();
    }

    @Override
    public String getName() {
        return name_;
    }

    public void setName(String name) {
        Objects.requireNonNull(name, "name");
        this.name_ = name;
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

    ConnectSelectorPool getConnectSelectorPool() {
        return connectSelectorPool_;
    }

    MessageIOSelectorPool getMessageIOSelectorPool() {
        return messageIOSelectorPool_;
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
