package net.ihiroky.niotty.buffer;

import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.TransportParameter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The implementation of {@link net.ihiroky.niotty.buffer.BufferSink} to write a file content
 * in a specified range to {@code java.nio.channels.WritableByteChannel} directly.
 * The {@code FileBufferSink} has header and footer as {@link net.ihiroky.niotty.buffer.CodecBuffer},
 * which are added by calling {@link #addFirst(CodecBuffer)} and {@link #addLast(CodecBuffer)}.
 * A lifecycle of a file in the {@code FileBufferSink} is managed byte a reference counting.
 * A reference count is incremented if new {@code FileBufferSink} is instantiated from a base {@code FileBufferSink},
 * and decremented if {@link #dispose()} is called. The initial (first) value is 1.
 * The file is closed when the value gets 0. A Lifecycle of header and footer is managed by themselves.
 * If this instance is sliced, duplicated and disposed, they are also change states respectively.
 *
 * @author Hiroki Itoh
 */
public class FileBufferSink implements BufferSink {

    private final FileChannel channel_;
    private long beginning_;
    private final long end_;
    private final TransportParameter attachment_;
    private CodecBuffer header_;
    private CodecBuffer footer_;
    private final AtomicInteger referenceCount_;

    FileBufferSink(FileChannel channel, long beginning, long length, TransportParameter attachment) {
        this(channel, beginning, length, attachment, null);
    }

    private FileBufferSink(FileChannel channel, long beginning, long length, TransportParameter attachment,
                           AtomicInteger referenceCount) {
        Objects.requireNonNull(channel, "channel");
        if (beginning < 0) {
            throw new IllegalArgumentException("beginning is negative.");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length is negative.");
        }

        if (referenceCount != null) {
            if (referenceCount.getAndIncrement() == 0) {
                throw new IllegalStateException("reference count is already 0.");
            }
        } else {
            referenceCount = new AtomicInteger(1);
        }

        channel_ = channel;
        beginning_ = beginning;
        end_ = beginning + length;
        attachment_ = (attachment != null) ? attachment : DefaultTransportParameter.NO_PARAMETER;
        referenceCount_ = referenceCount;

        header_ = Buffers.newCodecBuffer(0, attachment);
        footer_ = Buffers.newCodecBuffer(0, attachment);
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

    @Override
    public void transferTo(ByteBuffer buffer) {
        try {
            header_.transferTo(buffer);

            int limit = buffer.limit();
            buffer.limit(buffer.position() + (int) (end_ - beginning_));
            channel_.read(buffer, beginning_);
            buffer.limit(limit);
            beginning_ = end_;

            footer_.transferTo(buffer);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            dispose();
        }
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
            return true;
        } catch (IOException ioe) {
            dispose();
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
                header_ = new CodecBufferList(attachment(), buffer); // wrap and allow header to be added
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
                footer_ = new CodecBufferList(attachment(), buffer); // wrap and allow footer to be added
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
    public TransportParameter attachment() {
        return attachment_;
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
                contentSliced = new FileBufferSink(channel_, b, bytes, attachment_, referenceCount_);
                return (headerSliced == null)
                        ? contentSliced : Buffers.wrap(headerSliced, contentSliced, attachment_);
            }
            contentSliced = new FileBufferSink(channel_, b, end_, attachment_, referenceCount_);
            bytes -= contentRemaining;
            beginning_ = end_;
            if (bytes == 0) {
                return (headerSliced == null)
                        ? contentSliced : Buffers.wrap(headerSliced, contentSliced, attachment_);
            }
        }

        if (footerRemaining > 0) {
            if (bytes <= footerRemaining) {
                return newSlicedBufferSink(headerSliced, contentSliced, footer_.slice(bytes));
            }
            return newSlicedBufferSink(headerSliced, contentSliced, footer_.slice(footerRemaining));
        }

        return Buffers.newCodecBuffer(0, attachment_);
    }

    @Override
    public BufferSink duplicate() {
        return new FileBufferSink(channel_, beginning_, end_, attachment_, referenceCount_)
                .addFirst(header_.duplicate()).addLast(footer_.duplicate());
    }

    private BufferSink newSlicedBufferSink(BufferSink header, BufferSink content, BufferSink footer) {
        if (header == null) {
            if (content != null) {
                if (footer == null) {
                    return content;
                }
                return Buffers.wrap(content, footer, attachment_);
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
            BufferSink cdr = Buffers.wrap(content, footer);
            return Buffers.wrap(header, cdr, attachment_);
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
        return "(beginning:" + beginning_ + ", end:" + end_
                + ", channel:" + channel_ + ", referenceCount" + referenceCount_.get()
                + ", attachment:" + attachment_
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
