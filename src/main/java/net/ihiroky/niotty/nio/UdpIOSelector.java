package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Set;

/**
 * Created on 13/01/15, 15:35
 *
 * @author Hiroki Itoh
 */
public class UdpIOSelector extends AbstractSelector {

    private final ByteBuffer readBuffer_;
    private final ByteBuffer writeBuffer_; // TODO Use ByteBufferPool
    private Logger logger_ = LoggerFactory.getLogger(UdpIOSelector.class);

    private static final int MIN_BUFFER_SIZE = 256;

    UdpIOSelector(int readBufferSize, int writeBufferSize, boolean direct) {
        if (readBufferSize < MIN_BUFFER_SIZE) {
            readBufferSize = MIN_BUFFER_SIZE;
            logger_.warn("readBufferSize is set to {}.", readBufferSize);
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

            DatagramChannel channel = (DatagramChannel) key.channel();
            NioSocketTransport<?> transport = (NioSocketTransport<?>) key.attachment();
            try {
                if (channel.isConnected()) {
                    read = channel.read(localByteBuffer);
                    if (read == -1) {
                        if (logger_.isDebugEnabled()) {
                            logger_.debug("transport reaches the end of its stream:" + transport);
                        }
                        // TODO Discuss to call loadEvent(TransportEvent) and change ops to achieve have close
                        transport.doCloseSelectableChannel(true);
                        continue;
                    }
                    localByteBuffer.flip();
                    CodecBuffer buffer = Buffers.wrap(localByteBuffer);
                    transport.loadEvent(buffer);
                } else {
                    SocketAddress source = channel.receive(localByteBuffer);
                    if (source == null) {
                        if (logger_.isDebugEnabled()) {
                            logger_.debug("transport reaches the end of its stream:" + transport);
                        }
                        transport.doCloseSelectableChannel(true);
                        continue;
                    }
                    localByteBuffer.flip();
                    CodecBuffer buffer = Buffers.wrap(localByteBuffer);
                    transport.loadEvent(buffer, new DefaultTransportParameter(source));
                }
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
        final NioDatagramSocketTransport transport = (NioDatagramSocketTransport) context.transport();
        transport.readyToWrite(new AttachedMessage<>(input, context.transportParameter()));
        offerTask(new TaskLoop.Task() {
            @Override
            public int execute() throws Exception {
                try {
                    return transport.flush(writeBuffer_);
                } catch (IOException ioe) {
                    logger_.error("failed to flush buffer to " + transport, ioe);
                    transport.closeSelectableChannel();
                }
                return WAIT_NO_LIMIT;
            }
        });
    }
}
