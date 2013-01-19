package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StageContextAdapter;
import net.ihiroky.niotty.StageContextListener;
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

    private ByteBuffer byteBuffer;
    private Logger logger = LoggerFactory.getLogger(MessageIOSelector.class);

    private static final int MIN_BUFFER_SIZE = 256;

    static final StageContextListener<ByteBuffer> STORE_CONTEXT_LISTENER = new StageContextAdapter<ByteBuffer>() {
        @Override
        public void onProceed(PipeLine pipeLine, StageContext context, MessageEvent<ByteBuffer> event) {
            @SuppressWarnings("unchecked")
            NioChildChannelTransport<MessageIOSelector> transport =
                    (NioChildChannelTransport<MessageIOSelector>) event.getTransport();
            transport.readyToWrite(event.getMessage());
            transport.getSelector().offerTask(new FlushTask(transport));
        }

        @Override
        public void onProceed(PipeLine pipeLine, StageContext context, TransportStateEvent event) {
            NioChildChannelTransport transport = (NioChildChannelTransport) event.getTransport();
            switch (event.getState()) {
                case CONNECTED:
                    if (Boolean.FALSE.equals(event.getValue())) {
                        transport.closeSelectableChannel();
                    }
                    break;
                case BOUND:
                    if (Boolean.FALSE.equals(event.getValue())) {
                        transport.closeSelectableChannel();
                    }
                    break;
            }

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
        byteBuffer = direct ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocateDirect(bufferSize);
    }

    @Override
    protected void processSelectedKeys(Set<SelectionKey> selectedKeys) throws IOException {
        ByteBuffer localByteBuffer = byteBuffer;
        int read;

        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
            SelectionKey key = i.next();
            i.remove();

            SocketChannel channel = (SocketChannel) key.channel();
            while ((read = channel.read(localByteBuffer)) > 0) {}
            localByteBuffer.flip();

            // use wildcard to suppress unchecked warning, Actually, ? is MessageIOSelector
            NioChildChannelTransport<?> transport = (NioChildChannelTransport<?>) key.attachment();
            while (localByteBuffer.hasRemaining()) {
                loadEvent(new MessageEvent<ByteBuffer>(transport, localByteBuffer));
            }
            localByteBuffer.clear();
            if (read == -1) {
                // close current key and socket.
                transport.closeSelectableChannel();
            }
        }
    }

    private static class FlushTask implements Task<MessageIOSelector> {

        NioChildChannelTransport<MessageIOSelector> transport;

        FlushTask(NioChildChannelTransport<MessageIOSelector> transport) {
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

    static class BroadcastTask implements Task<MessageIOSelector> {

        ByteBuffer byteBuffer;

        BroadcastTask(ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
        }

        @Override
        public boolean execute(MessageIOSelector selector) {
            for (SelectionKey key : selector.keys()) {
                @SuppressWarnings("unchecked")
                NioChildChannelTransport<MessageIOSelector> transport =
                        (NioChildChannelTransport<MessageIOSelector>) key.attachment();
                transport.readyToWrite(byteBuffer.duplicate());
            }
            selector.offerTask(flushAllTask);
            return true;
        }
    }
}
