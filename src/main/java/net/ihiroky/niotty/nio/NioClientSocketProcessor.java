package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.Processor;

import java.util.Objects;

/**
 * Created on 13/01/18, 12:38
 *
 * @author Hiroki Itoh
 */
public class NioClientSocketProcessor implements Processor<NioClientSocketConfig> {

    private ConnectSelectorPool connectSelectorPool;
    private MessageIOSelectorPool messageIOSelectorPool;
    private String name;
    private int numberOfConnectThread;
    private int numberOfMessageIOThread;
    private int readBufferSize;
    private int writeBufferSize;
    private boolean useDirectBuffer;

    private static final int DEFAULT_NUMBER_OF_CONNECT_THREAD = 1;
    private static final int DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD =
            Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);
    private static final String DEFAULT_NAME = "NioServerSocket";
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final boolean DEFAULT_DIRECT_BUFFER = true;

    public NioClientSocketProcessor() {
        connectSelectorPool = new ConnectSelectorPool();
        messageIOSelectorPool = new MessageIOSelectorPool();
        name = "NioClientSocket";
        numberOfConnectThread = DEFAULT_NUMBER_OF_CONNECT_THREAD;
        numberOfMessageIOThread = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        readBufferSize = DEFAULT_BUFFER_SIZE;
        writeBufferSize = DEFAULT_BUFFER_SIZE;
        useDirectBuffer = DEFAULT_DIRECT_BUFFER;
    }

    @Override
    public NioClientSocketTransport createTransport(NioClientSocketConfig config) {
        return new NioClientSocketTransport(config, this);
    }

    @Override
    public void start() {
        messageIOSelectorPool.setReadBufferSize(readBufferSize);
        messageIOSelectorPool.setDirect(useDirectBuffer);
        messageIOSelectorPool.open(new NameCountThreadFactory(name.concat("-MessageIO")), numberOfMessageIOThread);
        connectSelectorPool.open(new NameCountThreadFactory(name.concat("-Connect")), numberOfConnectThread);
    }

    @Override
    public void stop() {
        messageIOSelectorPool.close();
        connectSelectorPool.close();
    }

    public void setName(String name) {
        Objects.requireNonNull(name, "name");
        this.name = name;
    }

    public void setNumberOfConnectThread(int numberOfConnectThread) {
        if (numberOfConnectThread <= 0) {
            throw new IllegalArgumentException("numberOfConnectThread must be positive.");
        }
        this.numberOfConnectThread = numberOfConnectThread;
    }

    public void setNumberOfMessageIOThread(int numberOfMessageIOThread) {
        if (numberOfMessageIOThread <= 0) {
            throw new IllegalArgumentException("numberOfMessageIOThread must be positive.");
        }
        this.numberOfMessageIOThread = numberOfMessageIOThread;
    }

    public void setReadBufferSize(int readBufferSize) {
        if (readBufferSize <= 0) {
            throw new IllegalArgumentException("readBufferSize must be positive.");
        }
        this.readBufferSize = readBufferSize;
    }

    public void setWriteBufferSize(int writeBufferSize) {
        if (writeBufferSize <= 0) {
            throw new IllegalArgumentException("writeBufferSize must be positive.");
        }
        this.writeBufferSize = writeBufferSize;
    }

    public void setUseDirectBuffer(boolean useDirectBuffer) {
        this.useDirectBuffer = useDirectBuffer;
    }

    ConnectSelectorPool getConnectSelectorPool() {
        return connectSelectorPool;
    }

    MessageIOSelectorPool getMessageIOSelectorPool() {
        return messageIOSelectorPool;
    }

    int getReadBufferSize() {
        return readBufferSize;
    }

    int getWriteBufferSize() {
        return writeBufferSize;
    }

    boolean isUseDirectBuffer() {
        return useDirectBuffer;
    }
}
