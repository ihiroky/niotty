package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.Pipeline;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StageContextAdapter;
import net.ihiroky.niotty.StageContextListener;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created on 13/01/15, 15:35
 *
 * @author Hiroki Itoh
 */
public class MessageIOSelector extends AbstractSelector<MessageIOSelector> {

    private ByteBuffer readBuffer_;
    private ByteBuffer writeBuffer_;
    private Logger logger_ = LoggerFactory.getLogger(MessageIOSelector.class);

    private static final int MIN_BUFFER_SIZE = 256;

    final StageContextListener<Object, BufferSink> storeContextListener_ =
            new StageContextAdapter<Object, BufferSink>() {
                @Override
                public void onProceed(
                        Pipeline pipeline, StageContext<Object, BufferSink> context, MessageEvent<BufferSink> event) {
                    NioChildChannelTransport transport = (NioChildChannelTransport) event.getTransport();
                    transport.writeBufferSink(event.getMessage());
                    transport.getEventLoop().offerTask(new FlushTask(transport, writeBuffer_));
                }

                @Override
                public void onProceed(
                        Pipeline pipeline, StageContext<Object, BufferSink> context, TransportStateEvent event) {
                    AbstractSelector.SELECTOR_STORE_CONTEXT_LISTENER.onProceed(pipeline, context, event);
                }
            };

    final Task<MessageIOSelector> flushAllTask_ = new Task<MessageIOSelector>() {
        @Override
        public boolean execute(MessageIOSelector selector) {
            NioChildChannelTransport transport;
            boolean finish = true;
            for (SelectionKey key : selector.keys()) {
                transport = (NioChildChannelTransport) key.attachment();
                try {
                    if (!transport.flush(writeBuffer_)) {
                        finish = false;
                    }
                } catch (IOException ioe) {
                    transport.closeSelectableChannel();
                    if (key.isValid()) {
                        // TODO log
                    }
                }
            }
            return finish;
        }
    };

    MessageIOSelector(int readBufferSize, int writeBufferSize, boolean direct) {
        if (readBufferSize < MIN_BUFFER_SIZE) {
            readBufferSize = MIN_BUFFER_SIZE;
            logger_.warn("readBufferSize is set to {}.", readBufferSize);
        }
        if (writeBufferSize < MIN_BUFFER_SIZE) {
            writeBufferSize = MIN_BUFFER_SIZE;
            logger_.warn("writeBufferSize is set to {}.", writeBufferSize);
        }
        readBuffer_ = direct ? ByteBuffer.allocateDirect(readBufferSize) : ByteBuffer.allocate(readBufferSize);
        writeBuffer_ = direct ? ByteBuffer.allocateDirect(writeBufferSize) : ByteBuffer.allocate(writeBufferSize);
    }

    @Override
    protected void processSelectedKeys(Set<SelectionKey> selectedKeys) throws IOException {
        ByteBuffer localByteBuffer = readBuffer_;
        int read;

        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
            SelectionKey key = i.next();
            i.remove();

            SocketChannel channel = (SocketChannel) key.channel();
            while ((read = channel.read(localByteBuffer)) > 0) {}
            localByteBuffer.flip();

            NioChildChannelTransport transport = (NioChildChannelTransport) key.attachment();
            transport.loadEvent(new MessageEvent<Object>(transport, Buffers.newCodecBuffer(localByteBuffer)));
            localByteBuffer.clear();
            if (read == -1) {
                // close current key and socket.
                transport.closeSelectableChannel();
            }
        }
    }

    public StageContextListener<Object, BufferSink> storeContextListener() {
        return storeContextListener_;
    }

    public Task<MessageIOSelector> flushAllTask() {
        return flushAllTask_;
    }

    void flushLater(NioChildChannelTransport transport) {
        offerTask(new FlushTask(transport, writeBuffer_));
    }

    static class FlushTask implements Task<MessageIOSelector> {

        NioChildChannelTransport transport_;
        ByteBuffer writeBuffer_;

        FlushTask(NioChildChannelTransport transport, ByteBuffer writeBuffer) {
            this.transport_ = transport;
            this.writeBuffer_ = writeBuffer;
        }

        @Override
        public boolean execute(MessageIOSelector selector) {
            try {
                return transport_.flush(writeBuffer_);
            } catch (IOException ioe) {
                transport_.closeSelectableChannel();
            }
            return true;
        }
    }
}
