package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractTransport;
import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.Stage;
import net.ihiroky.niotty.SuccessfulTransportFuture;
import net.ihiroky.niotty.Task;
import net.ihiroky.niotty.TaskLoopGroup;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.TimeUnit;

/**
 * A skeletal implementation of {@link net.ihiroky.niotty.Transport} for NIO.
 * @param <S> a type of selector
 */
public abstract class NioSocketTransport<S extends SelectLoop> extends AbstractTransport<S> {

    private SelectionKey key_;
    private static Logger logger_ = LoggerFactory.getLogger(NioSocketTransport.class);

    NioSocketTransport(
            String name, PipelineComposer pipelineComposer, TaskLoopGroup<S> taskLoopGroup) {
        super(name, pipelineComposer, taskLoopGroup);
    }

    @Override
    protected Stage ioStage() {
        return taskLoop().ioStage();
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
            closePipeline();
            return new SuccessfulTransportFuture(this);
        }
        selector.offer(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) {
                NioSocketTransport.this.doCloseSelectableChannel();
                return DONE;
            }
        });
        return closeFuture();
    }

    /**
     * Closes the channel.
     *
     * The key is cancelled and the channel is closed if the key is non null and valid.
     * The load pipeline and onStore pipeline (optional) is called
     * after the channel is closed. This method calls {@code #onCloseSelectableChannel} and
     * {@link #closePipeline()} after the channel close operation.
     * @return close future
     */
    final TransportFuture doCloseSelectableChannel() {
        DefaultTransportFuture closeFuture = closeFuture();
        if (key_ != null && key_.isValid() && closeFuture.executing()) {
            SelectableChannel channel = key_.channel();
            try {
                unregister(); // decrement register count
                channel.close();
                closeFuture.done();
            } catch (IOException e) {
                closeFuture.setThrowable(e);
            }

            pipeline().deactivate(DeactivateState.WHOLE);
            onCloseSelectableChannel();
            closePipeline();
        }
        return closeFuture;
    }

    void onCloseSelectableChannel() {
    }

    final SelectionKey key() {
        return key_;
    }

    void setInterestOp(int op) {
        int interestOps = key_.interestOps();
        if ((interestOps & op) == 0) {
            key_.interestOps(interestOps | op);
        }
    }

    void clearInterestOp(int op) {
        int interestOps = key_.interestOps();
        if ((interestOps & op) != 0) {
            key_.interestOps(interestOps & ~op);
        }
    }

    void register(final SelectableChannel channel, final int ops) throws IOException {
        if (key_ != null && key_.isValid()) {
            return;
        }

        final S loop = taskLoop();
        if (loop.isInLoopThread()) {
            if (ops == SelectionKey.OP_READ) {
                pipeline().activate();
            }
            key_ = loop.register(channel, ops, NioSocketTransport.this);
        } else {
            // case: ConnectorSelector <-> TcpIOSelector
            loop.offer(new Task() {
                @Override
                public long execute(TimeUnit timeUnit) {
                    try {
                        if (ops == SelectionKey.OP_READ) {
                            pipeline().activate();
                        }
                        key_ = loop.register(channel, ops, NioSocketTransport.this);
                    } catch (IOException ioe) {
                        logger_.warn("[register] Failed to register a channel:" + this, ioe);
                    }
                    return DONE;
                }
            });
        }
    }

    // Called only from doCloseSelectableChannel().
    // So there is no need to check the current thread.
    private void unregister() {
        key_.cancel();
        taskLoop().reject(this);
        logger_.debug("[unregister] {} is unregistered from {}.", this, Thread.currentThread());
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

    protected static CodecBuffer deepCopy(ByteBuffer bb) {
        int length = bb.limit();
        byte[] data = new byte[length];
        bb.get(data, 0, length);
        return Buffers.wrap(data, 0, length);
    }

    abstract void onSelected(SelectionKey key, SelectLoop selectLoop);
    abstract void readyToWrite(AttachedMessage<BufferSink> message);
    abstract void flush(ByteBuffer writeBuffer) throws IOException;
}
