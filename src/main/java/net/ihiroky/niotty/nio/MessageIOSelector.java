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

    private ByteBuffer byteBuffer_;
    private Logger logger_ = LoggerFactory.getLogger(MessageIOSelector.class);

    private static final int MIN_BUFFER_SIZE = 256;

    static final StageContextListener<Object, BufferSink> MESSAGE_IO_STORE_CONTEXT_LISTENER =
            new StageContextAdapter<Object, BufferSink>() {
                @Override
                public void onProceed(
                        Pipeline pipeline, StageContext<Object, BufferSink> context, MessageEvent<BufferSink> event) {
                    NioChildChannelTransport transport = (NioChildChannelTransport) event.getTransport();
                    try {
                        transport.writeBufferSink(event.getMessage());
                        transport.getEventLoop().offerTask(new FlushTask(transport));
                    } catch (IOException ioe) {
                        transport.closeSelectableChannel();
                    }
                }

                @Override
                public void onProceed(
                        Pipeline pipeline, StageContext<Object, BufferSink> context, TransportStateEvent event) {
                    AbstractSelector.SELECTOR_STORE_CONTEXT_LISTENER.onProceed(pipeline, context, event);
                }
            };

    static final Task<MessageIOSelector> flushAllTask = new Task<MessageIOSelector>() {
        @Override
        public boolean execute(MessageIOSelector selector) {
            NioChildChannelTransport transport;
            boolean finish = true;
            for (SelectionKey key : selector.keys()) {
                transport = (NioChildChannelTransport) key.attachment();
                try {
                    if (!transport.flush()) {
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

    MessageIOSelector(int bufferSize, boolean direct) {
        if (bufferSize < MIN_BUFFER_SIZE) {
            bufferSize = MIN_BUFFER_SIZE;
        }
        byteBuffer_ = direct ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocate(bufferSize);
    }

    @Override
    protected void processSelectedKeys(Set<SelectionKey> selectedKeys) throws IOException {
        ByteBuffer localByteBuffer = byteBuffer_;
        int read;

        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
            SelectionKey key = i.next();
            i.remove();

            SocketChannel channel = (SocketChannel) key.channel();
            while ((read = channel.read(localByteBuffer)) > 0) {}
            localByteBuffer.flip();

            NioChildChannelTransport transport = (NioChildChannelTransport) key.attachment();
            transport.loadEvent(new MessageEvent<Object>(transport, Buffers.newDecodeBuffer(localByteBuffer)));
            localByteBuffer.clear();
            if (read == -1) {
                // close current key and socket.
                transport.closeSelectableChannel();
            }
        }
    }

    public void flushLater(NioChildChannelTransport transport) {
        offerTask(new FlushTask(transport));
    }

    static class FlushTask implements Task<MessageIOSelector> {

        NioChildChannelTransport transport;

        FlushTask(NioChildChannelTransport transport) {
            this.transport = transport;
        }

        @Override
        public boolean execute(MessageIOSelector selector) {
            try {
                return transport.flush();
            } catch (IOException ioe) {
                transport.closeSelectableChannel();
            }
            return true;
        }
    }
}
