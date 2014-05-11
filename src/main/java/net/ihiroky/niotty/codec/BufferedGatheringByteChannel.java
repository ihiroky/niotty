package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.util.Arguments;
import net.ihiroky.niotty.util.Platform;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;

/**
 * The class implements a buffered gathering byte channel.
 *
 * By setting up such a channel, an application can write bytes
 * to the underlying channel without necessarily causing a call
 * to the underlying system for each byte written. The underlying
 * channel must not be non-blocking channel.
 *
 * The methods in this class is not synchronized.
 */
public class BufferedGatheringByteChannel implements GatheringByteChannel {

    /** The internal buffer where data is stored. */
    protected final ByteBuffer buffer_;

    /** The underlying channel. */
    private GatheringByteChannel channel_;

    /**
     * Constructs a new instance.
     *
     * The underlying channel must be set later by calling {@link #setUnderlyingChannel(java.nio.channels.GatheringByteChannel)}.
     *
     * @param size
     * @param directBuffer
     */
    protected BufferedGatheringByteChannel(int size, boolean directBuffer) {
        this(size, directBuffer, DefaultChannel.INSTANCE);
    }

    /**
     * Constructs a new instance.
     *
     * @param size the size of the buffer
     * @param directBuffer true if the buffer is direct buffer
     * @param underlyingChannel the underlying channel
     * @throws java.lang.IllegalArgumentException if the size is negative
     * @throws java.nio.channels.IllegalBlockingModeException if the underlying channel is non-blocking channel
     * @throws java.lang.NullPointerException if the underlyingChannel is null
     */
    public BufferedGatheringByteChannel(int size, boolean directBuffer, GatheringByteChannel underlyingChannel) {
        Arguments.requireNonNull(underlyingChannel, "underlyingChannel");
        checkNonBlocking(underlyingChannel);

        buffer_ = directBuffer ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
        channel_ = underlyingChannel;
    }

    private static void checkNonBlocking(GatheringByteChannel channel) {
        if (channel instanceof SelectableChannel) {
            SelectableChannel sc = (SelectableChannel) channel;
            if (!sc.isBlocking()) {
                throw new IllegalBlockingModeException();
            }
        }
    }

    /**
     * Closes the underlying channel without flushing the buffer.
     * @throws java.io.IOException if an I/O error occurs
     */
    protected void closeUnderlyingChannel() throws IOException {
        WritableByteChannel channel = channel_;
        if (channel != null) {
            channel.close();
        }
    }

    /**
     * Sets the new underlying channel.
     *
     * If the current underlying channel is open, then it is closed.
     * The buffer is not flushed by this method.
     *
     * @param underlyingChannel the underlying channel
     * @throws java.io.IOException if an I/O error occurs
     * @throws java.lang.NullPointerException if the underlyingChannel is null
     * @throws java.nio.channels.IllegalBlockingModeException if the underlying channel is non-blocking channel
     */
    protected void setUnderlyingChannel(GatheringByteChannel underlyingChannel) throws IOException {
        Arguments.requireNonNull(underlyingChannel, "channel");
        checkNonBlocking(underlyingChannel);

        GatheringByteChannel channel = channel_;
        if (channel.isOpen()) {
            channel.close();
        }
        channel_ = underlyingChannel;
    }

    /**
     * Flushes the buffer into the underlying channel.
     * @throws IOException an I/O error occurs
     */
    public void flush() throws IOException {
        flushPrivate();
    }

    private void flushPrivate() throws IOException {
        ByteBuffer buffer = buffer_;
        if (buffer.position() > 0) {
            buffer.flip();
            channel_.write(buffer);
            buffer.clear();
        }
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        long inputBytes = 0L;
        ByteBuffer buffer = buffer_;
        for (int i = 0; i < length; i++) {
            inputBytes += srcs[i + offset].remaining();
        }
        if (inputBytes <= buffer.capacity()) {
            if (inputBytes > buffer.remaining()) {
                flushPrivate();
            }
            writeBuffers(srcs, offset, length);
            return inputBytes;
        }

        flushPrivate();
        channel_.write(srcs, offset, length);
        return inputBytes;
    }

    private void writeBuffers(ByteBuffer[] srcs, int offset, int length) throws IOException {
        GatheringByteChannel channel = channel_;
        ByteBuffer buffer = buffer_;
        ByteBuffer src;
        for (int i = 0; i < length; i++) {
            src = srcs[i + offset];
            write(channel, buffer, src);
        }
    }

    @Override
    public long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        ByteBuffer buffer = buffer_;
        int writeBytes = src.remaining();

        write(channel_, buffer, src);

        return writeBytes;
    }

    private static void write(GatheringByteChannel channel, ByteBuffer buffer, ByteBuffer src) throws IOException {
        int nextRemaining = buffer.remaining() - src.remaining();
        while (nextRemaining <= 0) {
            int srcLimit = src.limit();
            src.limit(srcLimit + nextRemaining);
            buffer.put(src);
            buffer.flip();
            channel.write(buffer);
            buffer.clear();
            src.limit(srcLimit);
            nextRemaining = buffer.remaining() - src.remaining();
        }
        buffer.put(src);
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() {
        try {
            closeUnderlyingChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (buffer_.isDirect()) {
            Platform.release(buffer_);
        }
    }

    private static class DefaultChannel implements GatheringByteChannel {

        private static final DefaultChannel INSTANCE = new DefaultChannel();

        private static UnsupportedOperationException newUnsupportedException() {
            throw new UnsupportedOperationException("The underlying channel is not set.");
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            throw newUnsupportedException();
        }

        @Override
        public long write(ByteBuffer[] srcs) throws IOException {
            throw newUnsupportedException();
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            throw newUnsupportedException();
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public void close() throws IOException {
        }
    }
}
