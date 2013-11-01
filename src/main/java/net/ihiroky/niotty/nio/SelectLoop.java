package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.BufferSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of {@link net.ihiroky.niotty.TaskLoop} to handle {@link java.nio.channels.SelectableChannel}.
 */
public class SelectLoop extends TaskLoop implements StoreStage<BufferSink, Void> {

    private Selector selector_;
    private final AtomicBoolean wakenUp_;
    final ByteBuffer readBuffer_;
    final ByteBuffer writeBuffer_;
    final boolean copyReadBuffer;

    private static Logger logger_ = LoggerFactory.getLogger(SelectLoop.class);

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0).asReadOnlyBuffer();

    /**
     * Creates a new instance.
     */
    protected SelectLoop() {
        wakenUp_ = new AtomicBoolean();
        readBuffer_ = EMPTY_BUFFER;
        writeBuffer_ = EMPTY_BUFFER;
        copyReadBuffer = false;
    }

    protected SelectLoop(int readBufferSize, int writeBufferSize, boolean direct, boolean copyInput) {
        wakenUp_ = new AtomicBoolean();
        readBuffer_ = direct ? ByteBuffer.allocateDirect(readBufferSize) : ByteBuffer.allocate(readBufferSize);
        writeBuffer_ = direct ? ByteBuffer.allocateDirect(writeBufferSize) : ByteBuffer.allocate(writeBufferSize);
        copyReadBuffer = copyInput;
    }

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
    protected void poll(long timeout, TimeUnit timeUnit) throws Exception {
        int selected = (timeout == 0)
                ? selector_.selectNow()
                : selector_.select(Math.max(timeUnit.toMillis(timeout), 1));
        wakenUp_.set(false);
        if (selected > 0) {
            for (Iterator<SelectionKey> iterator = selector_.selectedKeys().iterator(); iterator.hasNext();) {
                SelectionKey key = iterator.next();
                iterator.remove();

                NioSocketTransport<?> transport = (NioSocketTransport<?>) key.attachment();
                transport.onSelected(key, this);
            }
        }
    }

    @Override
    protected void wakeUp() {
        if (wakenUp_.compareAndSet(false, true)) {
            selector_.wakeup();
        }
    }

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


    SelectionKey register(SelectableChannel channel, int ops, NioSocketTransport<?> transport) throws IOException {
        try {
            logger_.debug("[register] channel {} is registered to {}.", transport, Thread.currentThread());
            return channel.register(selector_, ops, transport);
        } catch (IOException ioe) {
            logger_.warn("[register] failed to register channel:" + channel, ioe);
            throw ioe;
        }
    }

    Set<SelectionKey> keys() {
        return selector_.keys();
    }

    boolean isOpen() {
        return (selector_ != null) && selector_.isOpen();
    }

    @Override
    public void store(StageContext<Void> context, BufferSink input) {
        final NioSocketTransport<?> transport = (NioSocketTransport<?>) context.transport();
        transport.readyToWrite(new AttachedMessage<BufferSink>(input, context.transportParameter()));
        try {
            transport.flush(writeBuffer_);
        } catch (IOException ioe) {
            logger_.warn("[store] Flush failed.", ioe);
            transport.doCloseSelectableChannel(true);
        }
    }

    @Override
    public void store(StageContext<Void> context, final TransportStateEvent event) {
        execute(event);
    }
}