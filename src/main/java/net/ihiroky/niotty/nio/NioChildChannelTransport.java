package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultPipeline;
import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.EventLoop;
import net.ihiroky.niotty.TransportConfig;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportState;
import net.ihiroky.niotty.event.TransportStateEvent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
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

    private boolean remainingPreviousData;
    private ByteBuffer writeBuffer;
    private Queue<BufferSink> pendingQueue;

    NioChildChannelTransport(TransportConfig config, String name, int writeBufferSize, boolean directWriteBuffer) {
        DefaultPipeline loadPipeline = DefaultPipeline.createLoadPipeLine(name);
        DefaultPipeline storePipeline = DefaultPipeline.createStorePipeLine(name);
        config.getPipelineInitializer().setUpPipeline(loadPipeline, storePipeline);
        loadPipeline.regulate();
        storePipeline.regulate();
        storePipeline.getLastContext().addListener(MessageIOSelector.MESSAGE_IO_STORE_CONTEXT_LISTENER);
        loadPipeline.verifyStageContextType();
        storePipeline.verifyStageContextType();

        ByteBuffer writeBuffer = directWriteBuffer
                ? ByteBuffer.allocateDirect(writeBufferSize) : ByteBuffer.allocate(writeBufferSize);

        setLoadPipeline(loadPipeline);
        setStorePipeline(storePipeline);
        this.writeBuffer = writeBuffer;
        this.pendingQueue = new LinkedList<>();
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
    public InetSocketAddress localAddress() {
        SelectableChannel channel = getSelectionKey().channel();
        try {
            return (InetSocketAddress) ((SocketChannel) channel).getLocalAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public InetSocketAddress remoteAddress() {
        SelectableChannel channel = getSelectionKey().channel();
        try {
            return (InetSocketAddress) ((SocketChannel) channel).getRemoteAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public boolean isOpen() {
        return getSelectionKey().channel().isOpen();
    }

    @Override
    public TransportFuture bind(SocketAddress socketAddress) {
        throw new UnsupportedOperationException("bind");
    }

    @Override
    public TransportFuture connect(SocketAddress remoteAddress) {
        throw new UnsupportedOperationException("connect");
    }

    @Override
    public TransportFuture close() {
        DefaultTransportFuture future = new DefaultTransportFuture(this);
        fire(new TransportStateEvent(this, TransportState.CONNECTED, future, null));
        return future;
    }

    @Override
    public TransportFuture join(InetAddress group, NetworkInterface networkInterface, InetAddress source) {
        throw new UnsupportedOperationException("join");
    }

    @Override
    protected void writeDirect(final BufferSink buffer) {
        EventLoop<MessageIOSelector> loop = getEventLoop();
        if (loop != null) {
            loop.offerTask(new EventLoop.Task<MessageIOSelector>() {
                @Override
                public boolean execute(MessageIOSelector eventLoop) throws Exception {
                    writeBufferSink(buffer);
                    eventLoop.flushLater(NioChildChannelTransport.this);
                    return true;
                }
            });
        }
    }

    void writeBufferSink(BufferSink buffer) throws IOException {
        WritableByteChannel channel = (WritableByteChannel) getSelectionKey().channel();
        ByteBuffer localWriteBuffer = writeBuffer;

        if (remainingPreviousData) {
            channel.write(localWriteBuffer);
            if (localWriteBuffer.hasRemaining()) {
                pendingQueue.offer(buffer);
                return;
            }
            remainingPreviousData = false;
            localWriteBuffer.clear();
        }

        if (pendingQueue.isEmpty()) {
            if (buffer.transferTo(channel, localWriteBuffer)) {
                localWriteBuffer.clear();
            } else {
                remainingPreviousData = true;
                pendingQueue.offer(buffer);
            }
        }
    }

    boolean flush() throws IOException {
        WritableByteChannel channel = (WritableByteChannel) getSelectionKey().channel();
        ByteBuffer localWriteBuffer = writeBuffer;

        if (remainingPreviousData) {
            channel.write(localWriteBuffer);
            if (localWriteBuffer.hasRemaining()) {
                return false;
            }
            remainingPreviousData = false;
            localWriteBuffer.clear();
        }

        for (BufferSink pendingBuffer; (pendingBuffer = pendingQueue.peek()) != null; ) {
            if (pendingBuffer.transferTo(channel, localWriteBuffer)) {
                localWriteBuffer.clear();
                pendingQueue.poll();
            } else {
                remainingPreviousData = true;
                return false;
            }
        }
        return true;
    }

    private void fire(MessageEvent<Object> event) {
        getStorePipeline().fire(event);
    }

    private void fire(TransportStateEvent event) {
        getStorePipeline().fire(event);
    }

    void loadEvent(MessageEvent<Object> event) {
        getLoadPipeline().fire(event);
    }

    void loadEventLater(final TransportStateEvent event) {
        MessageIOSelector selector = getEventLoop();
        if (selector != null) { // TODO Null Object
            selector.offerTask(new EventLoop.Task<MessageIOSelector>() {
                @Override
                public boolean execute(MessageIOSelector eventLoop) {
                    getLoadPipeline().fire(event);
                    return true;
                }
            });
        }
    }

}
