package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventDispatcher;
import net.ihiroky.niotty.Stage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.buffer.Packet;
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
 * An implementation of {@link net.ihiroky.niotty.EventDispatcher} to handle {@link java.nio.channels.SelectableChannel}.
 */
public class SelectDispatcher extends EventDispatcher {

    private Selector selector_;
    private final AtomicBoolean wakenUp_;
    final ByteBuffer readBuffer_;
    final ByteBuffer writeBuffer_;
    private final Stage ioStage_;

    private static Logger logger_ = LoggerFactory.getLogger(SelectDispatcher.class);

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0).asReadOnlyBuffer();

    /**
     * Creates a new instance with unbounded event queue.
     *
     * An invocation of this constructor behaves in exactly the same way as the invocation
     * {@code SelectDispatcher(0, 0, 0, false)}.
     */
    protected SelectDispatcher() {
        super(0);
        wakenUp_ = new AtomicBoolean();
        readBuffer_ = EMPTY_BUFFER;
        writeBuffer_ = EMPTY_BUFFER;
        ioStage_ = new IOStage(EMPTY_BUFFER);
    }

    /**
     * Creates a new instance.
     *
     * @param eventQueueCapacity the size of the event queue to buffer events;
     *                           less than or equal 0 to use unbounded queue
     * @param readBufferSize the size of read buffer
     * @param writeBufferSize the size of write buffer
     * @param direct true if the direct buffer is used
     */
    protected SelectDispatcher(int eventQueueCapacity, int readBufferSize, int writeBufferSize, boolean direct) {
        super(eventQueueCapacity);
        wakenUp_ = new AtomicBoolean();
        readBuffer_ = direct ? ByteBuffer.allocateDirect(readBufferSize) : ByteBuffer.allocate(readBufferSize);
        writeBuffer_ = direct ? ByteBuffer.allocateDirect(writeBufferSize) : ByteBuffer.allocate(writeBufferSize);
        ioStage_ = new IOStage(writeBuffer_);
    }

    Stage ioStage() {
        return ioStage_;
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
    protected void poll(long timeoutNanos) throws Exception {
        int selected = (timeoutNanos == 0)
                ? selector_.selectNow()
                : selector_.select(Math.max(TimeUnit.NANOSECONDS.toMillis(timeoutNanos), 1));
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

    private static class IOStage implements Stage {

        private final ByteBuffer writeBuffer_;

        IOStage(ByteBuffer writeBuffer) {
            writeBuffer_ = writeBuffer;
        }

        @Override
        public void loaded(StageContext context, Object message, Object parameter) {
            CodecBuffer buffer = (CodecBuffer) message;
            if (context.changesDispatcherOnProceed()) {
                CodecBuffer copy = Buffers.newCodecBuffer(buffer.remaining());
                copy.drainFrom(buffer);
                buffer.dispose();
                buffer = copy;
            }
            context.proceed(buffer, parameter);
        }

        @Override
        public void stored(StageContext context, Object message, Object parameter) {
            final NioSocketTransport<?> transport = (NioSocketTransport<?>) context.transport();
            transport.readyToWrite((Packet) message, parameter);
            try {
                transport.flush(writeBuffer_);
            } catch (IOException ioe) {
                logger_.warn("[stored] Flush failed.", ioe);
                writeBuffer_.clear();
                transport.doCloseSelectableChannel();
            }
        }

        @Override
        public void exceptionCaught(StageContext context, Exception exception) {
        }

        @Override
        public void activated(StageContext context) {
        }

        @Override
        public void deactivated(StageContext context) {
        }

        @Override
        public void eventTriggered(StageContext context, Object event) {
        }
    }
}
