package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.StoreStageContext;
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

/**
 * Created on 13/01/10, 17:56
 *
 * @author Hiroki Itoh
 */
public abstract class AbstractSelector<S extends AbstractSelector<S>> extends TaskLoop<S> {

    private Selector selector_;
    private SelectorStoreStage<S> selectorStoreStage_ = new SelectorStoreStage<>();

    private Logger logger_ = LoggerFactory.getLogger(AbstractSelector.class);

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
    protected void process(int waitTimeMillis) throws Exception {
        int selected;
        if (waitTimeMillis > 0) {
            selected = selector_.select(waitTimeMillis);
        } else if (waitTimeMillis == 0) {
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
            transport.loadEvent(new TransportStateEvent(TransportState.INTEREST_OPS, ops));
            processingMemberCount_.incrementAndGet();
            logger_.debug("channel {} is registered to {}.", channel, Thread.currentThread());
        } catch (IOException ioe) {
            logger_.warn("failed to register channel:" + channel, ioe);
        }
    }

    void unregister(SelectionKey key, NioSocketTransport<S> transport) {
        int ops = key.interestOps();
        key.cancel();
        transport.loadEvent(new TransportStateEvent(TransportState.INTEREST_OPS, ~ops));
        processingMemberCount_.decrementAndGet();
        logger_.debug("channel {} is unregistered from {}.", key.channel(), Thread.currentThread());
    }

    Set<SelectionKey> keys() {
        return selector_.keys();
    }

    boolean isOpen() {
        return (selector_ != null) && selector_.isOpen();
    }

    StoreStage<BufferSink, Void> ioStoreStage() {
        return selectorStoreStage_;
    }

    static class SelectorStoreStage<S extends AbstractSelector<S>> implements StoreStage<BufferSink, Void> {
        @Override
        public void store(StoreStageContext<BufferSink, Void> context, BufferSink input) {
        }

        @Override
        public void store(StoreStageContext<BufferSink, Void> context, final TransportStateEvent event) {
            @SuppressWarnings("unchecked")
            final NioSocketTransport<S> transport = (NioSocketTransport<S>) context.transport();
            switch (event.state()) {
                case CONNECTED: // fall through
                case BOUND:
                    if (transport.isInLoopThread()) {
                        close(transport, event);
                    } else {
                        transport.offerTask(new Task<S>() {
                            @Override
                            public int execute(S eventLoop) throws Exception {
                                close(transport, event);
                                return 0;
                            }
                        });
                    }
                    break;
                default:
                    break;
            }
        }

        private void close(NioSocketTransport<?> transport, TransportStateEvent event) {
            Object value = event.value();
            if (value == null || Boolean.FALSE.equals(value)) {
                transport.closeSelectableChannel();
            }
            event.future().done();
        }
    }
}
