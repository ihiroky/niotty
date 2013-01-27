package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.BusInterface;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.Transport;

/**
 * Created on 13/01/18, 12:38
 *
 * @author Hiroki Itoh
 */
public class NioClientSocketBusInterface implements BusInterface<NioClientSocketConfig> {

    private ConnectSelectorPool connectSelectorPool;
    private MessageIOSelectorPool messageIOSelectorPool;
    private NioClientSocketConfig config;

    public NioClientSocketBusInterface() {
        connectSelectorPool = new ConnectSelectorPool();
        messageIOSelectorPool = new MessageIOSelectorPool();
        config = new NioClientSocketConfig();
        config.setBaseName("NioClientSocket");
    }

    @Override
    public Transport createTransport() {
        return new NioClientSocketTransport(config, connectSelectorPool, messageIOSelectorPool);
    }

    @Override
    public NioClientSocketConfig getConfig() {
        return config;
    }

    @Override
    public void start() {
        messageIOSelectorPool.open(new NameCountThreadFactory(config.getBaseName() + "-MessageIO"),
                config.getNumberOfMessageIOThread(),
                config.getPipeLineFactory(),
                MessageIOSelector.STORE_CONTEXT_LISTENER);
        connectSelectorPool.open(new NameCountThreadFactory(config.getBaseName() + "-Connect"),
                config.getNumberOfConnectThread());
    }

    @Override
    public void stop() {
        messageIOSelectorPool.close();
        connectSelectorPool.close();
    }
}
