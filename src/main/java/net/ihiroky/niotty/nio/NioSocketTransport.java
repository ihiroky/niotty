package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractTransport;
import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.EventLoop;
import net.ihiroky.niotty.SucceededTransportFuture;
import net.ihiroky.niotty.TransportFuture;

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

    TransportFuture closeSelectableChannelLater() {
        S selector = getEventLoop();
        if (selector == null) {
            return new SucceededTransportFuture(this);
        }
        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        selector.offerTask(new EventLoop.Task<S>() {
            @Override
            public boolean execute(S eventLoop) throws Exception {
                closeSelectableChannel();
                future.done();
                return true;
            }
        });
        return future;
    }

    void closeSelectableChannel() {
        if (key != null) {
            SelectableChannel channel = key.channel();
            getEventLoop().unregister(key); // decrement register count
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
