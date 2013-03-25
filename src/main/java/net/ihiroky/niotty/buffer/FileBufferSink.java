package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * The implementation of {@link net.ihiroky.niotty.buffer.BufferSink} to write a file content
 * in a specified range to {@code java.nio.channels.WritableByteChannel} directly.
 *
 * @author Hiroki Itoh
 */
public class FileBufferSink implements BufferSink {

    private final FileChannel channel_;
    private long beginning_;
    private final long end_;
    private final int priority_;
    private final boolean autoClose_;

    FileBufferSink(FileChannel channel, long beginning, long end) {
        this(channel, beginning, end, Buffers.DEFAULT_PRIORITY, true);
    }

    FileBufferSink(FileChannel channel, long beginning, long end, int priority, boolean autoClose) {
        Objects.requireNonNull(channel, "channel");
        if (beginning < 0) {
            throw new IllegalArgumentException("beginning is negative.");
        }
        if (end < 0) {
            throw new IllegalArgumentException("end is negative.");
        }
        if (beginning > end) {
            throw new IllegalArgumentException("beginning is greater than end");
        }
        channel_ = channel;
        beginning_ = beginning;
        end_ = end;
        priority_ = priority;
        autoClose_ = autoClose;
    }

    private void close() {
        if (autoClose_) {
            try {
                channel_.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void closeAndThrow(RuntimeException e) {
        try {
            channel_.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        throw e;
    }

    /**
     * Writes a file content in the range given in the constructor to the {@code channel}.
     * The content is written into the {@code channel}, the file is automatically closed.
     * {@inheritDoc}
     */
    @Override
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        try {
            long remaining = end_ - beginning_;
            long transferred = channel_.transferTo(beginning_, remaining, channel);
            beginning_ += transferred;
            if (transferred < remaining) {
                return false;
            }
            close();
            return true;
        } catch (IOException ioe) {
            close();
            throw ioe;
        }
    }

    @Override
    public int remainingBytes() {
        return toNonNegativeInt(end_ - beginning_);
    }

    /**
     * The byte size of remaining data with long type in this instance.
     * @return the byte size of remaining data in this instance.
     */
    public long remainingBytesLong() {
        return end_ - beginning_;
    }

    @Override
    public int priority() {
        return priority_;
    }

    /**
     * Creates new {@code FileBufferSink} that shares this buffer's base content.
     * The beginning of the new {@code FileBufferSink} is the one of the this buffer.
     * The end of the new {@code FileBufferSink} is the {@code beginning + bytes}.
     * The two {@code FileBufferSink}'s beginning and end are independent.
     * After this method is called, the beginning of this buffer increases {@code bytes}.
     * The result of this method is auto closeable if and only if .
     *
     * @param bytes size of content to slice
     * @throws IllegalArgumentException if {@code bytes} exceeds this buffer's remaining.
     * @return the new {@code DecodeBuffer}
     */
    public FileBufferSink slice(int bytes) {
        if (bytes < 0 || bytes > remainingBytes()) {
            closeAndThrow(new IllegalArgumentException(
                    "Invalid input " + bytes + ". " + remainingBytes() + " byte remains."));
        }
        long b = beginning_;
        beginning_ += bytes;
        boolean autoClose = beginning_ == end_;
        return new FileBufferSink(channel_, b, b + bytes, priority_, autoClose);
    }

    static int toNonNegativeInt(long value) {
        return (value <= Integer.MAX_VALUE) ? (int) value : Integer.MAX_VALUE;
    }

    /**
     * Returns a summary of this buffer state.
     * @return a summary of this buffer state
     */
    @Override
    public String toString() {
        return "(beginning:" + beginning_ + ", end:" + end_
                + ", channel:" + channel_ + ", autoClose:" + autoClose_
                + ", priority:" + priority_
                + ')';
    }
}
