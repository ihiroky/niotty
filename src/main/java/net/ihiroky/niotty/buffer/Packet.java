package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.GatheringByteChannel;

/**
 * Writes data into a given channel or buffer.
 *
 * <p>This class has two member; startIndex and endIndex.
 * The startIndex shows the start index (included) of data to be written.
 * The endIndex shows the endIndex index (excluded) of data to be written.</p>
 *
 * <p>Add interface if new Transport is added and new data type is required.</p>
 */
public interface Packet {
    /**
     * Writes data between the startIndex and the endIndex to the given {@code GatheringByteChannel}.
     * The startIndex is increased by size of data which is written into the channel.
     *
     * @param channel the channel to be written into
     * @return true if the content in this buffer is written into the {@code channel}
     * @throws IOException if I/O operation is failed
     */
    boolean sink(GatheringByteChannel channel) throws IOException;

    /**
     * Writes data with udp target address between the startIndex and the endIndex
     * to the given {@code DatagramChannel}. The startIndex is increased by size of data
     * which is written into the channel.
     *
     *
     * @param channel the channel to be written into
     * @param buffer a support buffer to be used to send the content
     * @param target the target
     * @return true if all data in this buffer is written into the {@code channel}
     * @throws IOException if I/O operation is failed
     */
    boolean sink(DatagramChannel channel, ByteBuffer buffer, SocketAddress target) throws IOException;

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
    Packet addFirst(CodecBuffer buffer);

    /**
     * Adds a specified buffer after data which already exists in this instance.
     * @param buffer buffer to be added
     * @return this instance
     */
    Packet addLast(CodecBuffer buffer);

    /**
     * Creates a new {@code Packet} that shares the base content.
     * The startIndex of the new {@code Packet} is the one of the this instance.
     * The endIndex of the new {@code Packet} is {@code startIndex + bytes}.
     * The two {@code Packet}'s startIndex and endIndex are independent.
     * After this method is called, the startIndex of this instance increases {@code bytes}.
     *
     * @param bytes size of content to slice
     * @throws IllegalArgumentException if {@code bytes} exceeds this buffer's remaining.
     * @return the new {@code Packet}
     */
    Packet slice(int bytes);

    /**
     * Creates a new {@code Packet} that shares the base content.
     * The content of the new buffer will be that of this buffer. Changes to this buffer's content will be visible
     * in the new buffer, and vice versa; the two {@code Packet}s' startIndex and endIndex values will be independent.
     * The new buffer's startIndex and endIndex values will be identical to those of this buffer.
     *
     * @return the new {@code Packet}
     */
    Packet duplicate();

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
     * <p>Returns a byte buffer which has same content of this buffer.</p>
     * <p>This method may the create new byte buffer and copy the content in this buffer to the byte buffer.</p>
     * @return the byte buffer
     */
    ByteBuffer byteBuffer();

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
