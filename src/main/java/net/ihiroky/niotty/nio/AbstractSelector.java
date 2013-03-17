package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventLoop;
import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.StoreStageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.BufferSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

/**
 * Created on 13/01/10, 17:56
 *
 * @author Hiroki Itoh
 */
public abstract class AbstractSelector<S extends AbstractSelector<S>> extends EventLoop<S> {

    private Selector selector_;

    private Logger logger_ = LoggerFactory.getLogger(AbstractSelector.class);

    static final StoreStage<BufferSink, Void> SELECTOR_STORE_STAGE = new StoreStage<BufferSink, Void>() {
        @Override
        public void store(StoreStageContext<BufferSink, Void> context, BufferSink input) {
        }

        @Override
        public void store(StoreStageContext<BufferSink, Void> context, TransportStateEvent event) {
            NioSocketTransport<?> transport = (NioSocketTransport<?>) context.transport();
            Object value = event.value();
            switch (event.state()) {
                case ACCEPTED: // fall through
                case CONNECTED: // fall through
                case BOUND:
                    if (value == null || Boolean.FALSE.equals(value)) {
                        transport.closeSelectableChannel();
                    }
                    event.future().done();
                    break;
                default:
                    break;
            }
        }
    };

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
        int selected;
        if (timeout > 0) {
            selected = selector_.select(timeout);
        } else if (timeout == 0) {
            selected = selector_.selectNow();
        } else { // if (timeout < 0) {
            selected = selector_.select();
        }
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
            processingMemberCount_.incrementAndGet();
            logger_.debug("channel {} is registered to {}.", channel, Thread.currentThread());
        } catch (IOException ioe) {
            logger_.warn("failed to register channel:" + channel, ioe);
        }
    }

    void register(SelectableChannel channel, int ops, TransportFutureAttachment<S> attachment) {
        try {
            SelectionKey key = channel.register(selector_, ops, attachment);
            attachment.getTransport().setSelectionKey(key);
            processingMemberCount_.incrementAndGet();
            logger_.debug("channel {} is registered to {}.", channel, Thread.currentThread());
        } catch (IOException ioe) {
            logger_.warn("failed to register channel:" + channel, ioe);
        }
    }

    void unregister(SelectionKey key) {
        key.cancel();
        processingMemberCount_.decrementAndGet();
        logger_.debug("channel {} is unregistered from {}.", key.channel(), Thread.currentThread());
    }

    Set<SelectionKey> keys() {
        return selector_.keys();
    }

    public boolean isOpen() {
        return (selector_ != null) && selector_.isOpen();
    }
}
