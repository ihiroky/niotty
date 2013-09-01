package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportStateEvent;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.BufferSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created on 13/01/10, 17:56
 *
 * @author Hiroki Itoh
 */
public abstract class AbstractSelector extends TaskLoop implements StoreStage<BufferSink, Void> {

    private Selector selector_;

    private Logger logger_ = LoggerFactory.getLogger(AbstractSelector.class);

    /**
     * Creates a new instance.
     */
    protected AbstractSelector() {
        super();
    }

    @Override
    protected void onOpen() {
        try {
            selector_ = Selector.open();
        } catch (IOException e) {
            if (selector_ != null) {
                try {
                    selector_.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                selector_ = null;
            }
            throw new RuntimeException("failed to open selector.", e);
        }
    }

    @Override
    protected void poll(long timeout, TimeUnit timeUnit) throws Exception {
        long timeoutMillis = timeUnit.toMillis(timeout);
        int selected = (timeoutMillis == 0) ? selector_.selectNow()
                : (timeoutMillis > 0) ? selector_.select(timeoutMillis)
                : selector_.select();

        if (selected > 0) {
            processSelectedKeys(selector_.selectedKeys());
        }
    }

    @Override
    protected void wakeUp() {
        selector_.wakeup();
    }

    protected abstract void processSelectedKeys(Set<SelectionKey> selectedKeys) throws Exception;

    @Override
    protected void onClose() {
        for (SelectionKey key : selector_.keys()) {
            SelectableChannel channel = key.channel();
            try {
                key.cancel();
                channel.close();
            } catch (IOException ioe) {
                if (logger_.isDebugEnabled()) {
                    logger_.debug("failed to close registered channel : " + key, ioe);
                }
            }
        }

        try {
            selector_.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void register(SelectableChannel channel, int ops, NioSocketTransport<?> transport) {
        try {
            SelectionKey key = channel.register(selector_, ops, transport);
            transport.setSelectionKey(key);
            transport.loadEvent(new DefaultTransportStateEvent(TransportState.INTEREST_OPS, ops));
            logger_.debug("channel {} is registered to {}.", channel, Thread.currentThread());
        } catch (IOException ioe) {
            logger_.warn("failed to register channel:" + channel, ioe);
        }
    }

    void unregister(SelectionKey key, NioSocketTransport<?> transport) {
        key.cancel();
        transport.loadEvent(new DefaultTransportStateEvent(TransportState.INTEREST_OPS, 0));
        reject(transport);
        logger_.debug("channel {} is unregistered from {}.", key.channel(), Thread.currentThread());
    }

    Set<SelectionKey> keys() {
        return selector_.keys();
    }

    boolean isOpen() {
        return (selector_ != null) && selector_.isOpen();
    }

    @Override
    public void store(StageContext<Void> context, final TransportStateEvent event) {
        execute(event);
    }
}
