package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.*;
import net.ihiroky.niotty.buffer.Packet;
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
public abstract class NioSocketTransport extends AbstractTransport {

    private final DefaultPipeline pipeline_;
    private SelectionKey key_;
    private static Logger logger_ = LoggerFactory.getLogger(NioSocketTransport.class);

    NioSocketTransport(String name, PipelineComposer pipelineComposer, NioEventDispatcherGroup eventDispatcherGroup) {
        super(name, pipelineComposer, eventDispatcherGroup);

        Stage ioStage = ((NioEventDispatcher) eventDispatcher()).ioStage();
        pipeline_ = new DefaultPipeline(name, this, eventDispatcherGroup, Pipeline.IO_STAGE_KEY, ioStage);
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
        NioEventDispatcher dispatcher = (NioEventDispatcher) eventDispatcher();
        if (dispatcher == null) {
            pipeline_.close();
            return new SuccessfulTransportFuture(this);
        }
        dispatcher.offer(new Event() {
            @Override
            public long execute() {
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
     * closes its pipeline after the channel close operation.
     * @return close future
     */
    final TransportFuture doCloseSelectableChannel() {
        DefaultTransportFuture closeFuture = closeFuture();
        if (key_ != null && key_.isValid() && closeFuture.executing()) {
            SelectableChannel channel = key_.channel();
            try {
                unregister(); // deactivate and decrement register count
                channel.close();
                closeFuture.done();
            } catch (Exception e) {
                closeFuture.setThrowable(e);
            }

            onCloseSelectableChannel();
            pipeline_.close();
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

        final NioEventDispatcher dispatcher = (NioEventDispatcher)  eventDispatcher();
        if (dispatcher.isInDispatcherThread()) {
            if (ops == SelectionKey.OP_READ) {
                pipeline().activate();
            }
            key_ = dispatcher.register(channel, ops, NioSocketTransport.this);
        } else {
            // case: ConnectorSelector <-> TcpIOSelector
            dispatcher.offer(new Event() {
                @Override
                public long execute() {
                    try {
                        if (ops == SelectionKey.OP_READ) {
                            pipeline().activate();
                        }
                        key_ = dispatcher.register(channel, ops, NioSocketTransport.this);
                    } catch (Exception e) {
                        logger_.warn("[register] Failed to register a channel:" + this, e);
                    }
                    return DONE;
                }
            });
        }
    }

    // This method needs to be called by I/O thread.
    void unregister() {
        SelectionKey key = key_;
        if (key != null) {
            if ((key.interestOps() & SelectionKey.OP_READ) != 0) {
                try {
                    pipeline().deactivate();
                } catch (RuntimeException re) {
                    logger_.warn("[unregister] {}'s deactivation is failed.", this);
                }
            }
            key.cancel();
        }
        eventDispatcher().reject(this);
        logger_.debug("[unregister] {} is unregistered from {}.", this, Thread.currentThread());
    }

    protected void handleFlushStatus(FlushStatus status) {
        switch (status) {
            case FLUSHED:
                clearInterestOp(SelectionKey.OP_WRITE);
                return;
            case FLUSHING:
                clearInterestOp(SelectionKey.OP_WRITE);
                eventDispatcher().schedule(new Event() {
                    @Override
                    public long execute() throws Exception {
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

    abstract void onSelected(SelectionKey key, NioEventDispatcher selectDispatcher);
    abstract void readyToWrite(Packet message, Object parameter);
    abstract void flush(ByteBuffer writeBuffer) throws IOException;
}
