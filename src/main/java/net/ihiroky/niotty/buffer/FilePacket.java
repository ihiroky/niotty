package net.ihiroky.niotty.buffer;

import net.ihiroky.niotty.util.Arguments;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The implementation of {@link Packet} to write a file content
 * in a specified range to {@code java.nio.channels.WritableByteChannel} directly.
 * The {@code FilePacket} has header and footer as {@link CodecBuffer},
 * which are added by calling {@link #addFirst(CodecBuffer)} and {@link #addLast(CodecBuffer)}.
 * A lifecycle of a file in the {@code FilePacket} is managed byte a reference counting.
 * A reference count is incremented if new {@code FilePacket} is instantiated from a base {@code FilePacket},
 * and decremented if {@link #dispose()} is called. The initial (first) value is 1.
 * The file is closed when the value gets 0. A Lifecycle of header and footer is managed by themselves.
 * If this instance is sliced, duplicated and disposed, they are also change states respectively.
 *
 * @author Hiroki Itoh
 */
public class FilePacket extends AbstractPacket {

    private final FileChannel channel_;
    private long start_;
    private final long end_;
    private CodecBuffer header_;
    private CodecBuffer footer_;
    private final AtomicInteger referenceCount_;

    FilePacket(FileChannel channel, long start, long length) {
        this(channel, start, length, null);
    }

    private FilePacket(FileChannel channel, long start, long length, AtomicInteger referenceCount) {
        if (referenceCount != null) {
            if (referenceCount.getAndIncrement() == 0) {
                throw new IllegalStateException("reference count is already 0.");
            }
        } else {
            referenceCount = new AtomicInteger(1);
        }

        channel_ = Arguments.requireNonNull(channel, "channel");
        start_ = Arguments.requirePositiveOrZero(start, "start");
        end_ = start + Arguments.requirePositiveOrZero(length, "length");
        referenceCount_ = referenceCount;

        header_ = Buffers.newCodecBuffer(0);
        footer_ = Buffers.newCodecBuffer(0);
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
    public boolean sink(GatheringByteChannel channel) throws IOException {
        return header_.sink(channel) && transferFile(channel) && footer_.sink(channel);
    }

    @Override
    public void copyTo(ByteBuffer buffer) {
        try {
            header_.copyTo(buffer);

            int space = buffer.remaining();
            int remaining = (int) (end_ - start_);
            if (space >= remaining) {
                int limit = buffer.limit();
                buffer.limit(buffer.position() + remaining);
                channel_.read(buffer, start_);
                buffer.limit(limit);
            } else {
                throw new BufferOverflowException();
            }

            footer_.copyTo(buffer);
        } catch (IOException ioe) {
            dispose();
            throw new RuntimeException(ioe);
        }
    }

    private boolean transferFile(GatheringByteChannel channel) throws IOException {
        if (start_ == end_) {
            return true;
        }

        try {
            long remaining = end_ - start_;
            long transferred = channel_.transferTo(start_, remaining, channel);
            start_ += transferred;
            if (transferred < remaining) {
                return false;
            }
            return true;
        } catch (IOException ioe) {
            dispose();
            throw ioe;
        }
    }

    @Override
    public FilePacket addFirst(CodecBuffer buffer) {
        if (header_.remaining() > 0) {
            header_.addFirst(buffer);
        } else {
            Arguments.requireNonNull(buffer, "buffer");
            if (buffer.remaining() > 0) {
                header_ = new CodecBufferList(buffer); // wrap and allow header to be added
            }
        }
        return this;
    }

    @Override
    public FilePacket addLast(CodecBuffer buffer) {
        if (footer_.remaining() > 0) {
            footer_.addLast(buffer);
        } else {
            Arguments.requireNonNull(buffer, "buffer");
            if (buffer.remaining() > 0) {
                footer_ = new CodecBufferList(buffer); // wrap and allow footer to be added
            }
        }
        return this;
    }

    @Override
    public int remaining() {
        return toNonNegativeInt(remainingLong());
    }

    /**
     * The byte size of remaining data with long type in this instance.
     * @return the byte size of remaining data in this instance.
     */
    public long remainingLong() {
        return header_.remaining() + (end_ - start_) + footer_.remaining();
    }

    @Override
    public void dispose() {
        header_.dispose();
        if (referenceCount_.decrementAndGet() == 0) {
            try {
                channel_.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        footer_.dispose();
    }

    /**
     * Creates new {@code FilePacket} that shares this buffer's base content.
     * The startIndex of the new {@code FilePacket} is the one of the this buffer.
     * The endIndex of the new {@code FilePacket} is the {@code startIndex + bytes}.
     * The two {@code FilePacket}'s startIndex and endIndex are independent.
     * After this method is called, the startIndex of this buffer increases {@code bytes}.
     * The result of this method is auto closeable if and only if .
     *
     * @param bytes size of content to slice
     * @throws IllegalArgumentException if {@code bytes} exceeds this buffer's remaining.
     * @return the new {@code DecodeBuffer}
     */
    public Packet slice(int bytes) {
        int headerRemaining = header_.remaining();
        long contentRemaining = end_ - start_;
        int footerRemaining = footer_.remaining();
        if (bytes < 0 || bytes > (headerRemaining + contentRemaining + footerRemaining)) {
            closeAndThrow(new IllegalArgumentException("Invalid input " + bytes + ". "
                    + (headerRemaining + contentRemaining + footerRemaining) + " byte remains."));
        }

        Packet headerSliced = null;
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

        Packet contentSliced = null;
        if (contentRemaining > 0) {
            long b = start_;
            if (bytes <= contentRemaining) {
                start_ += bytes;
                contentSliced = new FilePacket(channel_, b, bytes, referenceCount_);
                return (headerSliced == null)
                        ? contentSliced : Buffers.wrap(headerSliced, contentSliced);
            }
            contentSliced = new FilePacket(channel_, b, end_, referenceCount_);
            bytes -= contentRemaining;
            start_ = end_;
            if (bytes == 0) {
                return (headerSliced == null)
                        ? contentSliced : Buffers.wrap(headerSliced, contentSliced);
            }
        }

        if (footerRemaining > 0) {
            if (bytes <= footerRemaining) {
                return newSlicedPacket(headerSliced, contentSliced, footer_.slice(bytes));
            }
            return newSlicedPacket(headerSliced, contentSliced, footer_.slice(footerRemaining));
        }
        return Buffers.newCodecBuffer(0);
    }

    @Override
    public Packet duplicate() {
        return new FilePacket(channel_, start_, end_, referenceCount_)
                .addFirst(header_.duplicate()).addLast(footer_.duplicate());
    }

    @Override
    public boolean hasArray() {
        return false;
    }

    @Override
    public byte[] array() {
        return byteBuffer().array();
    }

    @Override
    public int arrayOffset() {
        return 0;
    }

    private Packet newSlicedPacket(Packet header, Packet content, Packet footer) {
        if (header == null) {
            if (content != null) {
                if (footer == null) {
                    return content;
                }
                return Buffers.wrap(content, footer);
            }
            if (footer != null) {
                return footer;
            }
            throw new AssertionError("Runtime should not reach here");
        }

        if (content != null) {
            if (footer == null) {
                return Buffers.wrap(header, content);
            }
            Packet cdr = Buffers.wrap(content, footer);
            return Buffers.wrap(header, cdr);
        }
        if (footer != null) {
            return Buffers.wrap(header, footer);
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
        return "(startIndex:" + start_ + ", endIndex:" + end_
                + ", channel:" + channel_ + ", referenceCount" + referenceCount_.get()
                + ')';
    }

    /**
     * The reference count to control the lifecycle of this instance.
     * @return the reference count
     */
    public int referenceCount() {
        return referenceCount_.get();
    }

    CodecBuffer header() {
        return header_;
    }

    CodecBuffer footer() {
        return footer_;
    }
}
