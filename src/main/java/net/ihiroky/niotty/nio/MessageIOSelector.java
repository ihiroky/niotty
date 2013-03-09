package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.Pipeline;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StageContextAdapter;
import net.ihiroky.niotty.StageContextListener;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.TransportStateEvent;
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
                        Pipeline pipeline, StageContext<Object, BufferSink> context, BufferSink bufferSink) {
                    NioChildChannelTransport transport = (NioChildChannelTransport) context.transport();
                    transport.writeBufferSink(bufferSink);
                    transport.getEventLoop().offerTask(new FlushTask(transport, writeBuffer_));
                }

                @Override
                public void onProceed(
                        Pipeline pipeline, StageContext<Object, BufferSink> context, TransportStateEvent event) {
                    AbstractSelector.SELECTOR_STORE_CONTEXT_LISTENER.onProceed(pipeline, context, event);
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

        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext();) {
            SelectionKey key = i.next();
            i.remove();

            SocketChannel channel = (SocketChannel) key.channel();
            NioChildChannelTransport transport = (NioChildChannelTransport) key.attachment();
            try {
                read = channel.read(localByteBuffer);
                if (read == -1) {
                    if (logger_.isDebugEnabled()) {
                        logger_.debug("transport reaches the end of its stream:" + transport);
                    }
                    transport.closeSelectableChannel();
                    localByteBuffer.clear();
                    continue;
                }
            } catch (IOException ioe) {
                if (logger_.isDebugEnabled()) {
                    logger_.debug("failed to read from transport:", transport, ioe);
                }
                transport.closeSelectableChannel();
                localByteBuffer.clear();
                continue;
            }

            localByteBuffer.flip();
            transport.loadEvent(Buffers.newCodecBuffer(localByteBuffer));
            localByteBuffer.clear();
        }
    }

    public StageContextListener<Object, BufferSink> storeContextListener() {
        return storeContextListener_;
    }

    void flushLater(NioChildChannelTransport transport) {
        offerTask(new FlushTask(transport, writeBuffer_));
    }

    static class FlushTask implements Task<MessageIOSelector> {

        NioChildChannelTransport transport_;
        ByteBuffer writeBuffer_;

        static Logger logger_ = LoggerFactory.getLogger(FlushTask.class);

        FlushTask(NioChildChannelTransport transport, ByteBuffer writeBuffer) {
            this.transport_ = transport;
            this.writeBuffer_ = writeBuffer;
        }

        @Override
        public boolean execute(MessageIOSelector selector) {
            try {
                return transport_.flush(writeBuffer_);
            } catch (IOException ioe) {
                if (logger_.isDebugEnabled()) {
                    logger_.debug("failed to flush buffer to " + transport_, ioe);
                }
                transport_.closeSelectableChannel();
            }
            return true;
        }
    }
}
