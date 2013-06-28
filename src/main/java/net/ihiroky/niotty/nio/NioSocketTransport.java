package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractTransport;
import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.DefaultTransportStateEvent;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.SucceededTransportFuture;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportParameter;
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
public abstract class NioSocketTransport<S extends AbstractSelector> extends AbstractTransport<S> {

    private SelectionKey key_;

    /** Default weight to choose a TaskLoop. */
    static final int DEFAULT_WEIGHT = 1;

    NioSocketTransport(String name, PipelineComposer pipelineComposer, int weight) {
        super(name, pipelineComposer, weight);
    }

    @Override
    public String toString() {
        return (key_ != null) ? key_.channel().toString() : "unregistered";
    }

    final void setSelectionKey(SelectionKey key) {
        Objects.requireNonNull(key, "key");
        this.key_ = key;
    }

    final TransportFuture closeSelectableChannel() {
        S selector = taskLoop();
        if (selector == null) {
            closePipelines();
            return new SucceededTransportFuture(this);
        }
        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        executeStore(new TransportStateEvent(TransportState.CLOSED) {
            @Override
            public void execute() {
                NioSocketTransport.this.doCloseSelectableChannel(false);
                future.done();
            }
        });
        return future;
    }

    /**
     * Closes the channel.
     *
     * The key is cancelled and the channel is closed if the key is non null and valid.
     * {@link #executeLoad(net.ihiroky.niotty.TransportStateEvent)} and
     * {@link #executeStore(net.ihiroky.niotty.TransportStateEvent)} (optional) is called
     * after the channel is closed. This method calls {@code #onCloseSelectableChannel} and
     * {@link #closePipelines()} after the channel close operation.
     * @return succeeded future
     */
    final TransportFuture doCloseSelectableChannel(boolean executeStoreClosed) {
        if (key_ != null && key_.isValid()) {
            SelectableChannel channel = key_.channel();
            taskLoop().unregister(key_, this); // decrement register count
            try {
                channel.close();
                closeFuture().done();
            } catch (IOException e) {
                closeFuture().setThrowable(e);
            }
            TransportStateEvent event = new DefaultTransportStateEvent(TransportState.CLOSED, null);
            if (executeStoreClosed) {
                executeStore(event);
            }
            executeLoad(event);

        }
        onCloseSelectableChannel();
        closePipelines();
        return new SucceededTransportFuture(this);
    }

    void onCloseSelectableChannel() {
    }

    final void loadEvent(Object message) {
        executeLoad(message);
    }

    final void loadEvent(Object message, TransportParameter parameter) {
        executeLoad(message, parameter);
    }

    final void loadEvent(final TransportStateEvent event) {
        executeLoad(event);
    }

    final SelectionKey key() {
        return key_;
    }
}
