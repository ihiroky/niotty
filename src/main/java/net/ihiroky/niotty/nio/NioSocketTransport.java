package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractTransport;
import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.DefaultTransportStateEvent;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.SuccessfulTransportFuture;
import net.ihiroky.niotty.Task;
import net.ihiroky.niotty.TaskLoopGroup;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.util.Arguments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.TimeUnit;

/**
 * A skeletal implementation of {@link net.ihiroky.niotty.Transport} for NIO.
 * @param <S> a type of selector
 */
public abstract class NioSocketTransport<S extends AbstractSelector> extends AbstractTransport<S> {

    private SelectionKey key_;

    NioSocketTransport(
            String name, PipelineComposer pipelineComposer, TaskLoopGroup<S> taskLoopGroup) {
        super(name, pipelineComposer, taskLoopGroup);
    }

    @Override
    public String toString() {
        return (key_ != null) ? key_.channel().toString() : "unregistered";
    }

    final void setSelectionKey(SelectionKey key) {
        Arguments.requireNonNull(key, "key");
        this.key_ = key;
    }

    final TransportFuture closeSelectableChannel() {
        S selector = taskLoop();
        if (selector == null) {
            closePipelines();
            return new SuccessfulTransportFuture(this);
        }
        executeStore(new TransportStateEvent(TransportState.CLOSED) {
            @Override
            public long execute(TimeUnit timeUnit) {
                NioSocketTransport.this.doCloseSelectableChannel(false);
                return DONE;
            }
        });
        return closeFuture();
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
        DefaultTransportFuture closeFuture = closeFuture();
        if (key_ != null && key_.isValid()) {
            SelectableChannel channel = key_.channel();
            taskLoop().unregister(key_, this); // decrement register count
            try {
                channel.close();
                closeFuture.done();
            } catch (IOException e) {
                closeFuture.setThrowable(e);
            }
            TransportStateEvent event = new DefaultTransportStateEvent(TransportState.CLOSED, null);
            if (executeStoreClosed) {
                executeStore(event);
            }
            executeLoad(event);

        }
        onCloseSelectableChannel();
        closePipelines();
        return closeFuture;
    }

    void onCloseSelectableChannel() {
    }

    final SelectionKey key() {
        return key_;
    }

    private void setInterestOp(int op) {
        int interestOps = key_.interestOps();
        if ((interestOps & op) == 0) {
            key_.interestOps(interestOps | op);
        }
    }

    private void clearInterestOp(int op) {
        int interestOps = key_.interestOps();
        if ((interestOps & op) != 0) {
            key_.interestOps(interestOps & ~op);
        }
    }

    protected void handleFlushStatus(WriteQueue.FlushStatus status) {
        switch (status) {
            case FLUSHED:
                clearInterestOp(SelectionKey.OP_WRITE);
                return;
            case FLUSHING:
                clearInterestOp(SelectionKey.OP_WRITE);
                taskLoop().schedule(new Task() {
                    @Override
                    public long execute(TimeUnit timeUnit) throws Exception {
                        setInterestOp(SelectionKey.OP_WRITE);
                        return DONE;
                    }
                }, status.waitTimeMillis_, TimeUnit.MILLISECONDS);
                return;
            case SKIPPED:
                setInterestOp(SelectionKey.OP_WRITE);
                return;
            default:
                throw new AssertionError("Unexpected flush status: " + status);
        }
    }

    abstract void readyToWrite(AttachedMessage<BufferSink> message);

    abstract void flush(ByteBuffer writeBuffer) throws IOException;
}
