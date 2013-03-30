package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.StoreStageContext;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
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

    private final ByteBuffer readBuffer_;
    private final ByteBuffer writeBuffer_;
    private Logger logger_ = LoggerFactory.getLogger(MessageIOSelector.class);

    private static final int MIN_BUFFER_SIZE = 256;

    private final StoreStage<BufferSink, Void> messageIOStoreStage_;

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
        messageIOStoreStage_ = new MessageIOStoreStage(writeBuffer_);
    }

    @Override
    protected void processSelectedKeys(Set<SelectionKey> selectedKeys) throws IOException {
        ByteBuffer localByteBuffer = readBuffer_;
        int read;

        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext();) {
            SelectionKey key = i.next();
            i.remove();

            SocketChannel channel = (SocketChannel) key.channel();
            NioClientSocketTransport transport = (NioClientSocketTransport) key.attachment();
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
            } catch (ClosedByInterruptException ie) {
                if (logger_.isDebugEnabled()) {
                    logger_.debug("failed to read from transport by interruption:" + transport, ie);
                }
                transport.closeSelectableChannel();
                localByteBuffer.clear();
                continue;
            } catch (IOException ioe) {
                logger_.error("failed to read from transport:" + transport, ioe);
                transport.closeSelectableChannel();
                localByteBuffer.clear();
                continue;
            }

            localByteBuffer.flip();
            transport.loadEvent(Buffers.newCodecBuffer(localByteBuffer));
            localByteBuffer.clear();
        }
    }

    @Override
    public StoreStage<BufferSink, Void> ioStoreStage() {
        return messageIOStoreStage_;
    }

    private static class MessageIOStoreStage extends SelectorStoreStage<MessageIOSelector> {

        private final ByteBuffer writeBuffer_;

        static Logger logger_ = LoggerFactory.getLogger(MessageIOStoreStage.class);

        MessageIOStoreStage(ByteBuffer writeBuffer) {
            writeBuffer_ = writeBuffer;
        }

        @Override
        public void store(StoreStageContext<BufferSink, Void> context, BufferSink input) {
            final NioClientSocketTransport transport = (NioClientSocketTransport) context.transport();
            transport.writeBufferSink(input);
            if (transport.isInLoopThread()) {
                flush(transport, writeBuffer_);
            } else {
                transport.offerTask(new Task<MessageIOSelector>() {
                    @Override
                    public int execute(MessageIOSelector eventLoop) throws Exception {
                        return flush(transport, writeBuffer_);
                    }
                });
            }
        }

        public int flush(NioClientSocketTransport transport, ByteBuffer writeBuffer) {
            try {
                return transport.flush(writeBuffer);
            } catch (IOException ioe) {
                logger_.error("failed to flush buffer to " + transport, ioe);
                transport.closeSelectableChannel();
            }
            return 0;
        }
    }
}
