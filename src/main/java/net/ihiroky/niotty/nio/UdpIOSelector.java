package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.Task;
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
import java.util.concurrent.TimeUnit;

/**
 * Created on 13/01/15, 15:35
 *
 * TODO There is any need to check if content is remaining after the read operation ?
 */
public class UdpIOSelector extends AbstractSelector {

    private final ByteBuffer readBuffer_;
    private final ByteBuffer writeBuffer_; // TODO Use ByteBufferPool
    private final boolean duplicateBuffer_;
    private Logger logger_ = LoggerFactory.getLogger(UdpIOSelector.class);

    private static final int MIN_BUFFER_SIZE = 256;

    UdpIOSelector(int readBufferSize, int writeBufferSize, boolean direct, boolean duplicateBuffer) {
        if (readBufferSize < MIN_BUFFER_SIZE) {
            readBufferSize = MIN_BUFFER_SIZE;
            logger_.warn("readBufferSize is set to {}.", readBufferSize);
        }
        readBuffer_ = direct ? ByteBuffer.allocateDirect(readBufferSize) : ByteBuffer.allocate(readBufferSize);
        writeBuffer_ = direct ? ByteBuffer.allocateDirect(writeBufferSize) : ByteBuffer.allocate(writeBufferSize);
        duplicateBuffer_ = duplicateBuffer;
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
                if (key.isReadable()) {
                    if (channel.isConnected()) {
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
                        CodecBuffer buffer = duplicateBuffer_
                                ? duplicate(localByteBuffer) : Buffers.wrap(localByteBuffer, false);
                        transport.loadPipeline().execute(buffer);
                        localByteBuffer.clear();
                    } else {
                        SocketAddress source = channel.receive(localByteBuffer);
                        localByteBuffer.flip();
                        CodecBuffer buffer = duplicateBuffer_
                                ? duplicate(localByteBuffer) : Buffers.wrap(localByteBuffer, false);
                        transport.loadPipeline().execute(buffer, new DefaultTransportParameter(source));
                        localByteBuffer.clear();
                    }
                } else if (key.isWritable()) {
                    transport.flush(writeBuffer_);
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

    private CodecBuffer duplicate(ByteBuffer bb) {
        int length = bb.limit();
        byte[] data = new byte[length];
        bb.get(data, 0, length);
        return Buffers.wrap(data, 0, length);
    }

    @Override
    public void store(StageContext<Void> context, BufferSink input) {
        final NioDatagramSocketTransport transport = (NioDatagramSocketTransport) context.transport();
        transport.readyToWrite(new AttachedMessage<BufferSink>(input, context.transportParameter()));
        execute(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                transport.flush(writeBuffer_);
                return DONE;
            }
        });
    }
}
