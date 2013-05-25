package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractTransport;
import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.SucceededTransportFuture;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.TransportStateEvent;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Objects;

/**
 * Created on 13/01/11, 13:40
 * TODO fire TransportStateEvent on closed
 * @author Hiroki Itoh
 */
public abstract class NioSocketTransport<S extends AbstractSelector<S>> extends AbstractTransport<S> {

    private SelectionKey key_;

    @Override
    public String toString() {
        return (key_ != null) ? key_.channel().toString() : "unregistered";
    }

    final void setSelectionKey(SelectionKey key) {
        Objects.requireNonNull(key, "key");
        this.key_ = key;
    }

    final TransportFuture closeSelectableChannelLater(TransportState transportState) {
        S selector = getEventLoop();
        if (selector == null) {
            closePipelines();
            return new SucceededTransportFuture(this);
        }
        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        executeStore(new TransportStateEvent(transportState, null, future));
        return future;
    }

    /**
     * Closes the channel.
     *
     * The key is cancelled and the channel is closed if the key is non null and valid.
     * {@link net.ihiroky.niotty.TransportListener#onClose(net.ihiroky.niotty.Transport)} if it is registered and
     * {@link #executeLoad(net.ihiroky.niotty.TransportStateEvent)} is called after the channel is closed.
     * This method calls {@code #onCloseSelectableChannel} and {@link #closePipelines()} before the channel close
     * operation.
     * @return
     */
    final TransportFuture closeSelectableChannel() {
        onCloseSelectableChannel();
        closePipelines();
        if (key_ != null && key_.isValid()) {
            SelectableChannel channel = key_.channel();
            getEventLoop().unregister(key_, this); // decrement register count
            key_.cancel();
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            getTransportListener().onClose(this);
            executeLoad(new TransportStateEvent(TransportState.CONNECTED, null));
        }
        return new SucceededTransportFuture(this);
    }

    void onCloseSelectableChannel() {
    }

    final void loadEvent(Object message) {
        executeLoad(message);
    }

    final void loadEvent(final TransportStateEvent event) {
        executeLoad(event);
    }
}
