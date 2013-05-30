package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.StoreStage;
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
public class UdpIOSelector extends AbstractSelector<UdpIOSelector> {

    private final ByteBuffer readBuffer_;
    private final StoreStage<BufferSink, Void> ioStoreStage_;
    private Logger logger_ = LoggerFactory.getLogger(UdpIOSelector.class);

    private static final int MIN_BUFFER_SIZE = 256;

    UdpIOSelector(SelectorStoreStage<UdpIOSelector> ioStoreStage,
                  int readBufferSize, boolean direct) {
        if (readBufferSize < MIN_BUFFER_SIZE) {
            readBufferSize = MIN_BUFFER_SIZE;
            logger_.warn("readBufferSize is set to {}.", readBufferSize);
        }
        readBuffer_ = direct ? ByteBuffer.allocateDirect(readBufferSize) : ByteBuffer.allocate(readBufferSize);
        ioStoreStage_ = ioStoreStage;
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
                CodecBuffer buffer;
                if (channel.isConnected()) {
                    read = channel.read(localByteBuffer);
                    if (read == -1) {
                        if (logger_.isDebugEnabled()) {
                            logger_.debug("transport reaches the end of its stream:" + transport);
                        }
                        transport.closeSelectableChannel();
                        localByteBuffer.clear();
                        continue;
                    }
                    localByteBuffer.flip();
                    buffer = Buffers.wrap(localByteBuffer);
                    transport.loadEvent(buffer);
                } else {
                    SocketAddress source = channel.receive(localByteBuffer);
                    localByteBuffer.flip();
                    buffer = Buffers.wrap(localByteBuffer);
                    transport.loadEvent(buffer, new DefaultTransportParameter(source));
                }
            } catch (ClosedByInterruptException ie) {
                if (logger_.isDebugEnabled()) {
                    logger_.debug("failed to read from transport by interruption:" + transport, ie);
                }
                transport.closeSelectableChannel();
            } catch (IOException ioe) {
                logger_.error("failed to read from transport:" + transport, ioe);
                transport.closeSelectableChannel();
            } finally {
                localByteBuffer.clear();
            }
        }
    }

    @Override
    public StoreStage<BufferSink, Void> ioStoreStage() {
        return ioStoreStage_;
    }
}
