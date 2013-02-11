package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;

/**
 * A buffer class for decoding byte array. This interface exists for keeping symmetry against
 * {@link net.ihiroky.niotty.buffer.EncodeBuffer}. Implementations of this class has position, capacity.
 * for internal storage. The position shows a current reading byte position in the storage.
 * The capacity shows the end of the storage for valid operation.
 * <p></p>
 * {@link net.ihiroky.niotty.buffer.BufferSink} created from this class initially contains the data remaining in this
 * class for read. That is, {@code BufferSInk} contains the data in the position (included) and the capacity
 * (not included).
 *
 * @author Hiroki Itoh
 */
public interface DecodeBuffer {

    /**
     * Reads a byte from the buffer.
     *
     * @return a byte at current position.
     * @throws IndexOutOfBoundsException if current position exceeds the end of buffer
     */
    int    readByte();

    /**
     * Read bytes from the buffer into the specified {@code array}.
     *
     * @param bytes a byte array into which is data is written
     * @param offset first index in {@code bytes} which is written from;
     *               must be non-negative and less than or equal to {@code length}
     * @param length maximum number of bytes to be written into the {@code bytes};
     *               must be non-negative and less than or equal to {@code bytes.length - offset}
     * @throws java.lang.RuntimeException {@code offset} or {@code length} is invalid for the buffer
     */
    void   readBytes(byte[] bytes, int offset, int length);

    /**
     * Read specified end of bytes. The result is stored in {@code int} with right aligned big endian.
     * The buffer has {@code [0x30, 0x31, 0x32]} and call readBytes4(2), its result is 0x3031.
     * This method can return data up to 4 byte.
     *
     * @param bytes end of bytes to be read
     * @return int value which holds result bytes.
     * @throws IllegalArgumentException if the {@code bytes} is negative or more than 4 byte
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than {@code byte}
     */
    int    readBytes4(int bytes);

    /**
     * Read specified end of bytes. The result is stored in {@code int} with right aligned big endian.
     * The buffer has {@code [0x30, 0x31, 0x32]} and call readBytes8(2), its result is 0x3031 of long.
     * This method can return data up to 8 byte.
     *
     * @param bytes end of bytes to be read
     * @return long value which holds result bytes.
     * @throws IllegalArgumentException if the {@code bytes} is negative or more than 8 byte
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than {@code byte}
     */
    long   readBytes8(int bytes);

    /**
     * Reads char value from the buffer.
     *
     * @return char value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of char
     */
    char   readChar();

    /**
     * Reads short value from the buffer.
     *
     * @return short value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of short
     */
    short  readShort();

    /**
     * Reads int value from the buffer.
     *
     * @return int value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of int
     */
    int    readInt();

    /**
     * Reads long value from the buffer.
     *
     * @return long value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of long
     */
    long   readLong();

    /**
     * Reads float value from the buffer.
     *
     * @return float value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of float
     */
    float  readFloat();

    /**
     * Reads double value from the buffer.
     *
     * @return double value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of double
     */
    double readDouble();

    /**
     * Returns size of remaining data by byte.
     * This is equals to difference between the capacity and the current position.
     * @return size of remaining data by byte
     */
    int remainingBytes();

    /**
     * Returns size of capacity by byte.
     * @return size of capacity by byte
     */
    int capacityBytes();

    /**
     * Resets this buffer. The current position is set to 0, and the end is set to the buffer capacity.
     */
    void reset();

    /**
     * Creates {@link net.ihiroky.niotty.buffer.BufferSink} from this buffer contents.
     * @return {@code BufferSink} containing this buffer contents
     */
    BufferSink toBufferSink();

    /**
     * Converts remaining buffer contents to {@code java.nio.ByteBuffer}.
     * @return {@code java.nio.ByteBuffer}
     */
    ByteBuffer toByteBuffer();

    /**
     * Drains a specified {@code decodeBuffer}'s contents to this instance. The contents of {@code decodeBuffer} is
     * read by this instance and gets empty.
     * @param decodeBuffer data which is drained from.
     */
    void drainFrom(DecodeBuffer decodeBuffer);
}
