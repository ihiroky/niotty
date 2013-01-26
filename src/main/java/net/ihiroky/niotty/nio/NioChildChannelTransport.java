package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventLoop;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportState;
import net.ihiroky.niotty.event.TransportStateEvent;

import java.io.EOFException;
import java.io.IOException;
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
public class NioChildChannelTransport<S extends AbstractSelector<S>> extends NioSocketTransport<S> {

    private Queue<ByteBuffer> notWrittenBufferQueue; // TODO use ByteBuffer to handle pending bytes.

    NioChildChannelTransport() {
        this.notWrittenBufferQueue = new LinkedList<ByteBuffer>();
    }

    @Override
    public void write(Object message) {
        fire(new MessageEvent<Object>(this, message));
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
    }

    private void fire(MessageEvent<?> event) {
        AbstractSelector<S> selector = getSelector();
        if (selector != null) { // TODO Null Object
            selector.storeEvent(event);
        }
    }

    private void fire(TransportStateEvent event) {
        AbstractSelector<S> selector = getSelector();
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
        AbstractSelector<S> selector = getSelector();
        if (selector != null) { // TODO Null Object
            selector.offerTask(new EventLoop.Task<S>() {
                @Override
                public boolean execute(S eventLoop) {
                    eventLoop.loadEvent(event);
                    return true;
                }
            });

        }
    }

}
