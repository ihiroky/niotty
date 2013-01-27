package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractTransport;
import net.ihiroky.niotty.EventLoop;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Objects;

/**
 * Created on 13/01/11, 13:40
 *
 * @author Hiroki Itoh
 */
public abstract class NioSocketTransport<S extends AbstractSelector<S>> extends AbstractTransport<S> {

    private SelectionKey key;

    @Override
    public String toString() {
        return (key != null) ? key.channel().toString() : "unregistered";
    }

    void setSelectionKey(SelectionKey key) {
        Objects.requireNonNull(key, "key");
        this.key = key;
    }

    void unregisterLater() {
        S selector = getEventLoop();
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
        S selector = getEventLoop();
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

    protected final SelectionKey getSelectionKey() {
        return key;
    }
}
