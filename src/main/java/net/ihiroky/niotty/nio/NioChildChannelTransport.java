package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventLoop;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportState;
import net.ihiroky.niotty.event.TransportStateEvent;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created on 13/01/15, 16:50
 *
 * TODO abstracts ByteBuffer to to support 'zero copy' writing / reading (FileChannel)
 * @author Hiroki Itoh
 */
public class NioChildChannelTransport extends NioSocketTransport<MessageIOSelector> {

    private Queue<ByteBuffer> notWrittenBufferQueue; // TODO use ByteBuffer to handle pending bytes.

    NioChildChannelTransport() {
        this.notWrittenBufferQueue = new LinkedList<>();
        getTransportListener().onOpen(this);
    }

    @Override
    public void write(Object message) {
        fire(new MessageEvent<>(this, message));
    }

    @Override
    public void write(Object message, SocketAddress remote) {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public void bind(SocketAddress socketAddress) {
        throw new UnsupportedOperationException("bind");
    }

    @Override
    public void connect(SocketAddress remoteAddress) {
        throw new UnsupportedOperationException("connect");
    }

    @Override
    public void close() {
        fire(new TransportStateEvent(this, TransportState.CONNECTED, null));
        getTransportListener().onClose(this);
    }

    @Override
    public void join(InetAddress group, NetworkInterface networkInterface, InetAddress source) {
        throw new UnsupportedOperationException("join");
    }

    @Override
    protected void writeDirect(final ByteBuffer byteBuffer) {
        EventLoop<MessageIOSelector> loop = getEventLoop();
        if (loop != null) {
            loop.offerTask(new EventLoop.Task<MessageIOSelector>() {
                @Override
                public boolean execute(MessageIOSelector eventLoop) {
                    readyToWrite(byteBuffer);
                    eventLoop.flushLater(NioChildChannelTransport.this);
                    return true;
                }
            });
        }
    }

    private void fire(MessageEvent<?> event) {
        MessageIOSelector selector = getEventLoop();
        if (selector != null) { // TODO Null Object
            selector.storeEvent(event);
        }
    }

    private void fire(TransportStateEvent event) {
        MessageIOSelector selector = getEventLoop();
        if (selector != null) { // TODO Null Object
            selector.storeEvent(event);
        }
    }

    void readyToWrite(ByteBuffer byteBuffer) {
        notWrittenBufferQueue.offer(byteBuffer);
    }


    boolean flush() throws IOException {
        Queue<ByteBuffer> queue = notWrittenBufferQueue;
        WritableByteChannel channel = (WritableByteChannel) getSelectionKey().channel();
        int writeBytes;
        for (ByteBuffer buffer = queue.peek(); buffer != null; buffer = queue.poll()) {
            writeBytes = channel.write(buffer);
            if (writeBytes == -1) {
                throw new EOFException();
            }
            if (buffer.hasRemaining()) {
                return false;
            }
        }
        return true;
    }

    void loadEventLater(final TransportStateEvent event) {
        MessageIOSelector selector = getEventLoop();
        if (selector != null) { // TODO Null Object
            selector.offerTask(new EventLoop.Task<MessageIOSelector>() {
                @Override
                public boolean execute(MessageIOSelector eventLoop) {
                    eventLoop.loadEvent(event);
                    return true;
                }
            });
        }
    }

}
