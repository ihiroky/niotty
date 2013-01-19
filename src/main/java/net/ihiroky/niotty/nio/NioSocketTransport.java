package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventLoop;
import net.ihiroky.niotty.Transport;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Objects;

/**
 * Created on 13/01/11, 13:40
 *
 * @author Hiroki Itoh
 */
public abstract class NioSocketTransport<S extends AbstractSelector<S>> implements Transport {

    private AbstractSelector<S> selector;
    private SelectionKey key;

    @Override
    public String toString() {
        return (key != null) ? key.channel().toString() : "unregistered";
    }

    void setSelector(AbstractSelector<S> selector) {
        Objects.requireNonNull(selector, "selector");
        this.selector = selector;
    }

    void setSelectionKey(SelectionKey key) {
        Objects.requireNonNull(key, "key");
        this.key = key;
    }

    void unregisterLater() {
        if (selector != null) {
            selector.offerTask(new EventLoop.Task<S>() {
                @Override
                public boolean execute(S eventLoop) {
                    eventLoop.unregister(NioSocketTransport.this, key.channel(), key.interestOps());
                    return true;
                }
            });
        }
    }

    void closeLater() {
        if (selector != null) {
            selector.close(this);
        }
    }

    void closeSelectableChannel() {
        if (key != null) {
            SelectableChannel channel = key.channel();
            key.cancel();
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }

    protected final AbstractSelector<S> getSelector() {
        return selector;
    }

    protected final SelectionKey getSelectionKey() {
        return key;
    }
}
