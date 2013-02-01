package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.Processor;

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
    private boolean useDirectBuffer;

    public NioClientSocketProcessor() {
        connectSelectorPool = new ConnectSelectorPool();
        messageIOSelectorPool = new MessageIOSelectorPool();
        name = "NioClientSocket";
        numberOfConnectThread = 1;
        numberOfMessageIOThread = Runtime.getRuntime().availableProcessors() / 2;
        readBufferSize = 8192;
        useDirectBuffer = true;
    }

    @Override
    public NioClientSocketTransport createTransport(NioClientSocketConfig config) {
        return new NioClientSocketTransport(config, connectSelectorPool, messageIOSelectorPool);
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
        this.name = name;
    }

    public void setNumberOfConnectThread(int numberOfConnectThread) {
        this.numberOfConnectThread = numberOfConnectThread;
    }

    public void setNumberOfMessageIOThread(int numberOfMessageIOThread) {
        this.numberOfMessageIOThread = numberOfMessageIOThread;
    }

    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    public void setUseDirectBuffer(boolean useDirectBuffer) {
        this.useDirectBuffer = useDirectBuffer;
    }
}
