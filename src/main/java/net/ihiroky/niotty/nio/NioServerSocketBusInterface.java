package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.BusInterface;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.Transport;

/**
 * Created on 13/01/10, 14:37
 *
 * @author Hiroki Itoh
 */
public class NioServerSocketBusInterface implements BusInterface<NioServerSocketConfig> {

    private AcceptSelectorPool acceptSelectorPool;
    private MessageIOSelectorPool messageIOSelectorPool;
    private NioServerSocketConfig config;

    private static final int EXTERNAL_ACCEPTOR_POOL = -1;

    public NioServerSocketBusInterface() {
        this.acceptSelectorPool = new AcceptSelectorPool();
        this.messageIOSelectorPool = new MessageIOSelectorPool();
        this.config = new NioServerSocketConfig();
        config.setBaseName("NioServerSocket");
    }

    @Override
    public Transport createTransport() {
        return new NioServerSocketTransport(config, acceptSelectorPool, messageIOSelectorPool);
    }

    @Override
    public NioServerSocketConfig getConfig() {
        return config;
    }

    @Override
    public synchronized void start() {
        messageIOSelectorPool.setReadBufferSize(config.getChildReadBufferSize());
        messageIOSelectorPool.setDirect(config.isDirect());
        messageIOSelectorPool.open(new NameCountThreadFactory(config.getBaseName() + "-MessageIO"),
                config.getNumberOfMessageIOThread(),
                config.getLoadPipeLineFactory(),
                config.getStorePipeLineFactory(),
                MessageIOSelector.STORE_CONTEXT_LISTENER);
        acceptSelectorPool.open(new NameCountThreadFactory(config.getBaseName() + "-Accept"),
                config.getNumberOfAcceptThread());
    }

    @Override
    public synchronized void stop() {
        acceptSelectorPool.close();
        messageIOSelectorPool.close();
    }
}
