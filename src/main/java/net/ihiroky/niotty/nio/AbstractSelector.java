package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventLoop;
import net.ihiroky.niotty.Pipeline;
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

    private Selector selector_;
    private AtomicInteger registeredCount_;

    private Logger logger_ = LoggerFactory.getLogger(AbstractSelector.class);

    static final StageContextListener<Object, BufferSink> SELECTOR_STORE_CONTEXT_LISTENER =
            new StageContextAdapter<Object, BufferSink>() {
                @Override
                public void onProceed(
                        Pipeline pipeline, StageContext<Object, BufferSink> context, TransportStateEvent event) {
                    NioSocketTransport<?> transport = (NioSocketTransport<?>) event.getTransport();
                    Object value = event.getValue();
                    switch (event.getState()) {
                        case ACCEPTED: // fall through
                        case CONNECTED: // fall through
                        case BOUND:
                            if (value == null || Boolean.FALSE.equals(value)) {
                                transport.closeSelectableChannel();
                            }
                            event.getFuture().done();
                            break;
                        default:
                            break;
                    }
                }
            };

    AbstractSelector() {
        registeredCount_ = new AtomicInteger();
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
    protected void process(int timeout) throws Exception {
        int selected = (timeout <= 0) ? selector_.select() : selector_.select(timeout);
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


    void register(SelectableChannel channel, int ops, NioSocketTransport<S> transport) {
        try {
            SelectionKey key = channel.register(selector_, ops, transport);
            transport.setSelectionKey(key);
            registeredCount_.incrementAndGet();
            logger_.debug("channel {} is registered to {}.", channel, Thread.currentThread());
        } catch (IOException ioe) {
            logger_.warn("failed to register channel:" + channel, ioe);
        }
    }

    void register(SelectableChannel channel, int ops, TransportFutureAttachment<S> attachment) {
        try {
            SelectionKey key = channel.register(selector_, ops, attachment);
            attachment.getTransport().setSelectionKey(key);
            registeredCount_.incrementAndGet();
            logger_.debug("channel {} is registered to {}.", channel, Thread.currentThread());
        } catch (IOException ioe) {
            logger_.warn("failed to register channel:" + channel, ioe);
        }
    }

    void unregister(SelectionKey key) {
        key.interestOps(0);
        registeredCount_.decrementAndGet();
        logger_.debug("channel {} is unregistered from {}.", key.channel(), Thread.currentThread());
    }

    int getRegisteredCount() {
        return registeredCount_.get();
    }

    Set<SelectionKey> keys() {
        return selector_.keys();
    }

    public boolean isOpen() {
        return (selector_ != null) && selector_.isOpen();
    }
}
