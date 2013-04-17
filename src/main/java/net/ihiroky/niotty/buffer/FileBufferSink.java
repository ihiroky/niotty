package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
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
    private final boolean autoDispose_;
    private CodecBuffer header_;
    private CodecBuffer footer_;

    FileBufferSink(FileChannel channel, long beginning, long length, int priority) {
        this(channel, beginning, length, priority, true);
    }

    private FileBufferSink(FileChannel channel, long beginning, long length, int priority, boolean autoDispose) {
        Objects.requireNonNull(channel, "channel");
        if (beginning < 0) {
            throw new IllegalArgumentException("beginning is negative.");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length is negative.");
        }
        channel_ = channel;
        beginning_ = beginning;
        end_ = beginning + length;
        priority_ = priority;
        autoDispose_ = autoDispose;

        CodecBuffer empty = Buffers.newCodecBuffer(0, priority);
        header_ = empty;
        footer_ = empty;
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
     * If headers and footers added by {@link #addFirst(CodecBuffer)} and {@link #addLast(CodecBuffer)} exist,
     * then the headers, the file content and the footers is written in this order.
     * The content is written into the {@code channel}, the file is automatically closed.
     * {@inheritDoc}
     */
    @Override
    public boolean transferTo(GatheringByteChannel channel) throws IOException {
        return header_.transferTo(channel) && transferFile(channel) && footer_.transferTo(channel);
    }

    private boolean transferFile(GatheringByteChannel channel) throws IOException {
        if (beginning_ == end_) {
            return true;
        }
        try {
            long remaining = end_ - beginning_;
            long transferred = channel_.transferTo(beginning_, remaining, channel);
            beginning_ += transferred;
            if (transferred < remaining) {
                return false;
            }
            if (autoDispose_) {
                dispose();
            }
            return true;
        } catch (IOException ioe) {
            if (autoDispose_) {
                dispose();
            }
            throw ioe;
        }
    }

    @Override
    public FileBufferSink addFirst(CodecBuffer buffer) {
        if (header_.remainingBytes() > 0) {
            header_.addFirst(buffer);
        } else {
            Objects.requireNonNull(buffer, "buffer");
            if (buffer.remainingBytes() > 0) {
                header_ = buffer;
            }
        }
        return this;
    }

    @Override
    public FileBufferSink addLast(CodecBuffer buffer) {
        if (footer_.remainingBytes() > 0) {
            footer_.addLast(buffer);
        } else {
            Objects.requireNonNull(buffer, "buffer");
            if (buffer.remainingBytes() > 0) {
                footer_ = buffer;
            }
        }
        return this;
    }

    @Override
    public int remainingBytes() {
        return toNonNegativeInt(remainingBytesLong());
    }

    /**
     * The byte size of remaining data with long type in this instance.
     * @return the byte size of remaining data in this instance.
     */
    public long remainingBytesLong() {
        return header_.remainingBytes() + (end_ - beginning_) + footer_.remainingBytes();
    }

    @Override
    public int priority() {
        return priority_;
    }

    @Override
    public void dispose() {
        header_.dispose();
        try {
            channel_.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        footer_.dispose();
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
    public BufferSink slice(int bytes) {
        int headerRemaining = header_.remainingBytes();
        long contentRemaining = end_ - beginning_;
        int footerRemaining = footer_.remainingBytes();
        if (bytes < 0 || bytes > (headerRemaining + contentRemaining + footerRemaining)) {
            closeAndThrow(new IllegalArgumentException("Invalid input " + bytes + ". "
                    + (headerRemaining + contentRemaining + footerRemaining) + " byte remains."));
        }

        BufferSink headerSliced = null;
        if (headerRemaining > 0) {
            if (bytes <= headerRemaining) {
                return header_.slice(bytes);
            }
            headerSliced = header_.slice(headerRemaining);
            bytes -= headerRemaining;
            if (bytes == 0) {
                return headerSliced;
            }
        }

        BufferSink contentSliced = null;
        if (contentRemaining > 0) {
            long b = beginning_;
            if (bytes <= contentRemaining) {
                beginning_ += bytes;
                contentSliced = new FileBufferSink(channel_, b, bytes, priority_, false);
                return (headerSliced == null) ? contentSliced : Buffers.newBufferSink(headerSliced, contentSliced);
            }
            contentSliced = new FileBufferSink(channel_, b, end_, priority_, true);
            bytes -= contentRemaining;
            beginning_ = end_;
            if (bytes == 0) {
                return (headerSliced == null) ? contentSliced : Buffers.newBufferSink(headerSliced, contentSliced);
            }
        }

        if (footerRemaining > 0) {
            if (bytes <= footerRemaining) {
                return newSlicedBufferSink(headerSliced, contentSliced, footer_.slice(bytes));
            }
            return newSlicedBufferSink(headerSliced, contentSliced, footer_.slice(footerRemaining));
        }

        return Buffers.newCodecBuffer(0, priority_);
    }

    private BufferSink newSlicedBufferSink(BufferSink header, BufferSink content, BufferSink footer) {
        if (header == null) {
            if (content != null) {
                if (footer == null) {
                    return content;
                }
                return Buffers.newBufferSink(content, footer, priority_);
            }
            if (footer != null) {
                return footer;
            }
            throw new AssertionError("Runtime should not reach here");
        }

        if (content != null) {
            if (footer == null) {
                return Buffers.newBufferSink(header, content);
            }
            BufferSink cdr = Buffers.newBufferSink(content, footer);
            return Buffers.newBufferSink(header, cdr, priority_);
        }
        if (footer != null) {
            return Buffers.newBufferSink(header, footer);
        }
        return header;
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
                + ", channel:" + channel_ + ", autoClose:" + autoDispose_
                + ", priority:" + priority_
                + ')';
    }

    CodecBuffer header() {
        return header_;
    }

    CodecBuffer footer() {
        return footer_;
    }
}
