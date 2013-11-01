package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;

/**
 * Writes data into a given {@code java.nio.channel.WritableByteChannel} or {@code java.nio.ByteBuffer}.
 *
 * <p>This class has two member; startIndex and endIndex.
 * The startIndex shows the start index (included) of data to be written.
 * The endIndex shows the endIndex index (excluded) of data to be written.</p>
 *
 * <p>Add interface if new Transport is added and new data type is required.</p>
 *
 * @author Hiroki Itoh
 */
public interface BufferSink {
    /**
     * Writes data between the startIndex and the endIndex to the given {@code channel}.
     * The startIndex is increased by size of data which is written into the channel.
     *
     * @param channel the {@code WritableByteChannel} to be written into
     * @return true if all data in this instance is written into the {@code channel}
     * @throws IOException if I/O operation is failed
     */
    boolean transferTo(GatheringByteChannel channel) throws IOException;

    /**
     * Copies data between the startIndex and the endIndex to the given {@code buffer}.
     * The startIndex and endIndex of this object is not changed.
     *
     * @param buffer the buffer to be written into
     * @throws java.nio.BufferOverflowException if {@link #remaining()} is larger than the buffer's remaining.
     */
    void copyTo(ByteBuffer buffer);

    /**
     * Adds a specified buffer before data which already exists in this instance.
     * @param buffer buffer to be added
     * @return this instance
     */
    BufferSink addFirst(CodecBuffer buffer);

    /**
     * Adds a specified buffer after data which already exists in this instance.
     * @param buffer buffer to be added
     * @return this instance
     */
    BufferSink addLast(CodecBuffer buffer);


    /**
     * Creates a new {@code BufferSink} that shares the base content.
     * The startIndex of the new {@code BufferSink} is the one of the this instance.
     * The endIndex of the new {@code BufferSink} is {@code startIndex + bytes}.
     * The two {@code BufferSink}'s startIndex and endIndex are independent.
     * After this method is called, the startIndex of this instance increases {@code bytes}.
     *
     * @param bytes size of content to slice
     * @throws IllegalArgumentException if {@code bytes} exceeds this buffer's remaining.
     * @return the new {@code BufferSink}
     */
    BufferSink slice(int bytes);

    /**
     * Creates a new {@code BufferSink} that shares the base content.
     * The content of the new buffer will be that of this buffer. Changes to this buffer's content will be visible
     * in the new buffer, and vice versa; the two {@code BufferSink}s' startIndex and endIndex values will be independent.
     * The new buffer's startIndex and endIndex values will be identical to those of this buffer.
     *
     * @return the new {@code BufferSink}
     */
    BufferSink duplicate();

    /**
     * Returns true if this buffer is backed by a byte array.
     * @return true if this buffer is backed by a byte array
     */
    boolean hasArray();

    /**
     * <p>Returns a byte array that backs this buffer if {@link #hasArray()} returns true,
     * or returns a copy of a content in this buffer as a byte array.</p>
     * <p>Modification to this buffer's content modifies the backed byte array, and vice versa
     * as long as this buffer does not expands its size.</p>
     *
     * @return a byte array that backs this buffer, or a copy of a content in this buffer.
     */
    byte[] array();

    /**
     * <p>Returns an offset for a first byte in byte array that backs this buffer if {@link #hasArray()} returns true,
     * or returns 0.</p>
     * @return an offset for a first byte in byte array that backs this buffer
     */
    int arrayOffset();

    /**
     * The byte size of remaining data in this instance.
     * @return the byte size of remaining data in this instance.
     */
    int remaining();

    /**
     * Disposes resources managed in this class if exists.
     */
    void dispose();
}
