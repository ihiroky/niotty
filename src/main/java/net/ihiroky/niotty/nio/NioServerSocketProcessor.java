package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.Processor;
import net.ihiroky.niotty.Transport;

import java.util.Objects;

/**
 * Created on 13/01/10, 14:37
 *
 * @author Hiroki Itoh
 */
public class NioServerSocketProcessor implements Processor<NioServerSocketConfig> {

    private AcceptSelectorPool acceptSelectorPool;
    private MessageIOSelectorPool messageIOSelectorPool;

    private String name;
    private int numberOfAcceptThread;
    private int numberOfMessageIOThread;
    private int readBufferSize;
    private boolean useDirectBuffer;

    private static final int DEFAULT_NUMBER_OF_ACCEPT_THREAD = 1;
    private static final int DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD =
            Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);
    private static final String DEFAULT_NAME = "NioServerSocket";
    private static final int DEFAULT_READ_BUFFER_SIZE = 8192;
    private static final boolean DEFAULT_DIRECT_BUFFER = true;

    public NioServerSocketProcessor() {
        acceptSelectorPool = new AcceptSelectorPool();
        messageIOSelectorPool = new MessageIOSelectorPool();

        numberOfAcceptThread = DEFAULT_NUMBER_OF_ACCEPT_THREAD;
        numberOfMessageIOThread = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        name = DEFAULT_NAME;
        readBufferSize = DEFAULT_READ_BUFFER_SIZE;
        useDirectBuffer = DEFAULT_DIRECT_BUFFER;
    }

    @Override
    public synchronized void start() {
        messageIOSelectorPool.setReadBufferSize(readBufferSize);
        messageIOSelectorPool.setDirect(useDirectBuffer);
        messageIOSelectorPool.open(new NameCountThreadFactory(name.concat("-MessageIO")),
                numberOfMessageIOThread);
        acceptSelectorPool.open(new NameCountThreadFactory(name.concat("-Accept")),
                numberOfAcceptThread);
    }

    @Override
    public synchronized void stop() {
        acceptSelectorPool.close();
        messageIOSelectorPool.close();
    }

    @Override
    public Transport createTransport(NioServerSocketConfig config) {
        return new NioServerSocketTransport(config, acceptSelectorPool, messageIOSelectorPool);
    }

    public void setName(String name) {
        Objects.requireNonNull(name, "name");
        this.name = name;
    }

    public void setNumberOfAcceptThread(int numberOfAcceptThread) {
        if (numberOfAcceptThread <= 0) {
            throw new IllegalArgumentException("numberOfAcceptThread must be positive.");
        }
        this.numberOfAcceptThread = numberOfAcceptThread;
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

    public void setUseDirectBuffer(boolean useDirectBuffer) {
        this.useDirectBuffer = useDirectBuffer;
    }
}
