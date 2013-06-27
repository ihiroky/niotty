package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Set;

/**
 * Created on 13/01/15, 15:35
 *
 * @author Hiroki Itoh
 */
public class TcpIOSelector extends AbstractSelector<TcpIOSelector> {

    private final ByteBuffer readBuffer_;
    private Logger logger_ = LoggerFactory.getLogger(TcpIOSelector.class);

    private static final int MIN_BUFFER_SIZE = 256;

    TcpIOSelector(int readBufferSize, boolean direct) {
        if (readBufferSize < MIN_BUFFER_SIZE) {
            readBufferSize = MIN_BUFFER_SIZE;
            logger_.warn("readBufferSize is set to {}.", readBufferSize);
        }
        readBuffer_ = direct ? ByteBuffer.allocateDirect(readBufferSize) : ByteBuffer.allocate(readBufferSize);
    }

    @Override
    protected void processSelectedKeys(Set<SelectionKey> selectedKeys) throws IOException {
        ByteBuffer localByteBuffer = readBuffer_;
        int read;

        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext();) {
            SelectionKey key = i.next();
            i.remove();

            ReadableByteChannel channel = (ReadableByteChannel) key.channel();
            NioSocketTransport<?> transport = (NioSocketTransport<?>) key.attachment();
            try {
                read = channel.read(localByteBuffer);
                if (read == -1) {
                    if (logger_.isDebugEnabled()) {
                        logger_.debug("transport reaches the end of its stream:" + transport);
                    }
                    // TODO Discuss to call loadEvent(TransportEvent) and change ops to achieve have close
                    transport.doCloseSelectableChannel(true);
                    localByteBuffer.clear();
                    continue;
                }

                localByteBuffer.flip();
                transport.loadEvent(Buffers.wrap(localByteBuffer));
            } catch (ClosedByInterruptException ie) {
                if (logger_.isDebugEnabled()) {
                    logger_.debug("failed to read from transport by interruption:" + transport, ie);
                }
                transport.doCloseSelectableChannel(true);
            } catch (IOException ioe) {
                logger_.error("failed to read from transport:" + transport, ioe);
                transport.doCloseSelectableChannel(true);
            } finally {
                localByteBuffer.clear();
            }
        }
    }

    @Override
    public void store(StageContext<Void> context, BufferSink input) {
        final NioClientSocketTransport transport = (NioClientSocketTransport) context.transport();
        transport.readyToWrite(new AttachedMessage<>(input, context.transportParameter()));
        offerTask(new TaskLoop.Task<TcpIOSelector>() {
            @Override
            public int execute(TcpIOSelector eventLoop) throws Exception {
                try {
                    return transport.flush();
                } catch (IOException ioe) {
                    logger_.error("failed to flush buffer to " + transport, ioe);
                    transport.closeSelectableChannel();
                }
                return WAIT_NO_LIMIT;
            }
        });
    }
}
