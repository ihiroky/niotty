package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventLoop;
import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StageContextAdapter;
import net.ihiroky.niotty.StageContextListener;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 13/01/10, 17:56
 *
 * @author Hiroki Itoh
 */
public abstract class AbstractSelector<S extends AbstractSelector<S>> extends EventLoop<S> {

    private Selector selector;
    private AtomicInteger registeredCount;

    private Logger logger = LoggerFactory.getLogger(AbstractSelector.class);

    static final StageContextListener<Object, BufferSink> SELECTOR_STORE_CONTEXT_LISTENER =
            new StageContextAdapter<Object, BufferSink>() {
                @Override
                public void onProceed(
                        PipeLine pipeLine, StageContext<Object, BufferSink> context, TransportStateEvent event) {
                    NioServerSocketTransport transport = (NioServerSocketTransport) event.getTransport();
                    Object value = event.getValue();
                    switch (event.getState()) {
                        case ACCEPTED:
                            // fall through
                        case CONNECTED:
                            // fall through
                        case BOUND:
                            if (value == null || Boolean.FALSE.equals(value)) {
                                transport.closeSelectableChannel();
                            }
                            break;
                        default:
                            break;
                    }
                }
            };

    AbstractSelector() {
        registeredCount = new AtomicInteger();
    }

    @Override
    protected void onOpen() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            if (selector != null) {
                try {
                    selector.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                selector = null;
            }
            throw new RuntimeException("failed to open selector.", e);
        }
    }

    @Override
    protected void process(int timeout) throws Exception {
        int selected = (timeout <= 0) ? selector.select() : selector.select(timeout);
        if (selected > 0) {
            processSelectedKeys(selector.selectedKeys());
        }
    }

    @Override
    protected void wakeUp() {
        selector.wakeup();
    }

    protected abstract void processSelectedKeys(Set<SelectionKey> selectedKeys) throws Exception;

    @Override
    protected void onClose() {
        for (SelectionKey key : selector.keys()) {
            SelectableChannel channel = key.channel();
            try {
                key.cancel();
                channel.close();
            } catch (IOException ioe) {
                if (logger.isDebugEnabled()) {
                    logger.debug("failed to close registered channel : " + key, ioe);
                }
            }
        }

        try {
            selector.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void register(final NioSocketTransport<S> transport, final SelectableChannel channel, final int ops) {
        try {
            SelectionKey key = channel.register(selector, ops, transport);
            transport.setSelectionKey(key);
            registeredCount.incrementAndGet();
            logger.debug("channel {} is registered to {}.", channel, Thread.currentThread());
        } catch (IOException ioe) {
            logger.warn("failed to register channel:" + channel, ioe);
        }
    }

    void unregister(NioSocketTransport<S> transport, final SelectableChannel channel, final int ops) {
        try {
            channel.register(selector, channel.validOps() & (~ops));
            registeredCount.decrementAndGet();
            logger.debug("channel {} is unregistered from {}.", channel, Thread.currentThread());
        } catch (IOException ioe) {
            logger.warn("failed to unregisterLater channel:" + channel, ioe);
        }
    }

    void close(final NioSocketTransport<S> transport) {
        offerTask(new Task<S>() {
            @Override
            public boolean execute(S loop) {
                transport.closeSelectableChannel();
                return true;
            }
        });
    }

    int getRegisteredCount() {
        return registeredCount.get();
    }

    Set<SelectionKey> keys() {
        return selector.keys();
    }

    public boolean isOpen() {
        return (selector != null) && selector.isOpen();
    }
}
