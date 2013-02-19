package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;

/**
 * A buffer class for decoding byte array. This interface exists for keeping symmetry against
 * {@link net.ihiroky.niotty.buffer.EncodeBuffer}. Implementations of this class has position, limit.
 * for internal storage. The position shows a current reading byte position in the storage.
 * The limit shows the end of the storage for valid operation.
 * <p></p>
 * his class supports a signed integer encoding with variable length (Variable Byte Codes). See
 * {@link net.ihiroky.niotty.buffer.EncodeBuffer} for detail.
 * <p></p>
 * {@link net.ihiroky.niotty.buffer.BufferSink} created from this class initially contains the data remaining in this
 * class for read. That is, {@code BufferSink} contains the data in the position (included) and the limit.
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
     * Reads bytes from the buffer into the specified {@code array}.
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
     * Reads bytes from the buffer.
     * If the {@code byteBuffer} has enough space to read the whole buffer data, the {@code byteBuffer} gets
     * all. If not, the {@code byteBuffer} is filled with the part of buffer data.
     *
     * @param byteBuffer a {@code ByteBuffer} into which is data is written
     */
    void   readBytes(ByteBuffer byteBuffer);

    /**
     * Reads specified end of bytes. The result is stored in {@code int} with right aligned big endian.
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
     * Reads specified end of bytes. The result is stored in {@code int} with right aligned big endian.
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
     * Reads {@code Integer or Long} value in signed VBC form from the buffer. The result may be null.
     * @return {@code Integer or Long} value read from the buffer
     */
    Number readVariableByteNumber();

    /**
     * Reads {@code long} value in signed VBC form from the buffer. The null value is returned as (negative) zero.
     * @return {@code long} value read from the buffer
     */
    long readVariableByteLong();

    /**
     * Reads {@code int} value in signed VBC form from the buffer. The null value is returned as (negative) zero.
     * @return {@code int} value read from the buffer
     */
    int readVariableByteInteger();

    /**
     * Reads a string from the buffer using a specified {@code charsetDecoder}.
     * If some {@code java.nio.charset.CharacterCodingException} happens, this method throws
     * {@code java.lang.RuntimeException} which has {@code CharacterCodingException} as its cause.
     *
     * @param charsetDecoder decoder to decode byte data
     * @throws java.lang.RuntimeException if an error happens
     * @return string value read from the buffer
     */
    String readString(CharsetDecoder charsetDecoder);

    /**
     * Skips specified bytes of the buffer.
     *
     * The actual number of {@code n} of bytes to be skipped is the smaller of {@code bytes} and
     * {@link #remainingBytes()}, and {@code n - remainingBytes() >= 0}. The value {@code n} is added to the position
     * and then {@code n} is returned.
     *
     * @param bytes the number of bytes to be skipped
     * @return the actual number of bytes skipped
     */
    int skipBytes(int bytes);

    /**
     * Returns size of remaining data by byte.
     * This is equals to difference between the limit and the current position.
     * @return size of remaining data by byte
     */
    int remainingBytes();

    /**
     * Returns size of the limit by byte.
     * @return size of the limit by byte
     */
    int limitBytes();

    /**
     * Clears this buffer. The current position is set to 0.
     */
    void clear();

    /**
     * Creates {@link net.ihiroky.niotty.buffer.BufferSink} from this buffer contents.
     * @return {@code BufferSink} containing this buffer contents
     */
    BufferSink toBufferSink();

    /**
     * Converts remaining buffer contents to {@code java.nio.ByteBuffer}.
     * An internal storage in this buffer is shared with the result {@code ByteBuffer}. If some data is written into
     * the {@code ByteBuffer}, then this buffer is also modified and vice versa.
     * @return {@code java.nio.ByteBuffer}
     */
    ByteBuffer toByteBuffer();

    /**
     * Returns true if this buffer is backed by a byte array.
     * @return true if this buffer is backed by a byte array
     */
    boolean hasArray();

    /**
     * Returns a byte array that backs this buffer.
     * Modification to this buffer's content modifies the byte array, and vice versa.
     * If {@link #hasArray()} returns false, this method throws {@code java.lang.UnsupportedOperationException}.
     * @return a byte array that backs this buffer
     */
    byte[] toArray();

    /**
     * Returns an offset for a first byte in byte array that backs this buffer.
     * If {@link #hasArray()} returns false, this method throws {@code java.lang.UnsupportedOperationException}.
     * @return an offset for a first byte in byte array that backs this buffer
     * @throws UnsupportedOperationException if this buffer is not backed by a byte array
     */
    int arrayOffset();

    /**
     * Drains a specified {@code decodeBuffer}'s contents to this instance. The {@code decodeBuffer} is read
     * by this instance and gets empty.
     *
     * @param decodeBuffer buffer which is drained from.
     */
    void drainFrom(DecodeBuffer decodeBuffer);
}
