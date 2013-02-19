package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;

/**
 * A buffer class for encoding byte array. Implementations of this class has position and limit.
 * for internal storage. The position shows a current writing byte position in the storage.
 * The data written into this class exists between index 0 and position - 1. The limit is the end of the storage
 * for valid operation. The storage is automatically expanded if the position exceeds the limit for a writing
 * operation.
 * <p></p>
 * This class supports a signed integer encoding with variable length, signed VBC (Variable Byte Codes). The encoding
 * has an end bit, a sign bit and data bits. Each MSB per byte is the end bit. The end bit in last byte is 1,
 * otherwise 0. The sign bit is second bit from MSB in the first byte. If a value to be encoded is positive or zero,
 * then its sign bit is 0. If negative, then 1. The other bits are the data bits with little endian. The data bits
 * in first byte is 6 bits, and in the other byte is 7 bits. The data in the data bits is its magnitude,
 * not complement of two. This encoding support {@code null} value as negative zero; data bits show zero
 * and the sign bit and the end bit is 1 in single byte. the example of encoding -229 as binary format is
 * {@code 01100101 10000011}; the second bit from MSB in first byte is 1 for negative, the MSB the second byte is 1
 * for its last byte, and The data bits in first byte shows 37 and in second byte shows 192 (128 + 64).
 * <p></p>
 * Unsigned integer encoding with variable length is known as Variable Byte Codes
 * (<a href="http://nlp.stanford.edu/IR-book/html/htmledition/variable-byte-codes-1.html"></a>).
 * <p></p>
 * {@link net.ihiroky.niotty.buffer.BufferSink} created from this class initially contains the data written into
 * this class.
 *
 * @author Hiroki Itoh
 */
public interface EncodeBuffer {

    /**
     * Writes a {@code value} as byte.
     *
     * @param value byte value
     */
    void writeByte(int value);

    /**
     * Writes a specified byte array.
     *
     * @param bytes the byte array to be written
     * @param offset first position in the {@code byte} to be written from;
     *               must be non-negative and less than {@code bytes.length}
     * @param length byte size to be written from {@code offset};
     *               must be non-negative and less than or equal to {@code bytes.length - offset}
     */
    void writeBytes(byte[] bytes, int offset, int length);

    /**
     * Writes a specified {@code java.nio.ByteBuffer}.
     *
     * @param byteBuffer the byte array to be written
     */
    void writeBytes(ByteBuffer byteBuffer);

    /**
     * Writes a specified {@code bits}, which byte size is specified {@code bytes}. The {@code bits} is right-aligned.
     * The {@code bits} is written into this buffer with big endian.
     *
     * @param bits the set of bit to be written
     * @param bytes byte size of the {@code bits}; must be non-positive and less than or equal to 4
     */
    void writeBytes4(int bits, int bytes);

    /**
     * Writes a specified {@code bits}, which byte size is specified {@code bytes}. The {@code bits} is right-aligned.
     * The {@code bits} is written into this buffer with big endian.
     *
     * @param bits the set of bit to be written
     * @param bytes byte size of the {@code bits}; must be non-positive and less than or equal to 8
     */
    void writeBytes8(long bits, int bytes);

    /**
     * Writes a specified short {@code value}. The value si written into this buffer with two byte big endian.
     * @param value the number of short type
     */
    void writeShort(short value);

    /**
     * Writes a specified char {@code value}. The value si written into this buffer with two byte big endian.
     * @param value the number of char type
     */
    void writeChar(char value);

    /**
     * Writes a specified int {@code value}. The value si written into this buffer with four byte big endian.
     * @param value the number of int type
     */
    void writeInt(int value);

    /**
     * Writes a specified long {@code value}. The value is written into this buffer with eight byte big endian.
     * @param value the number of long type
     */
    void writeLong(long value);

    /**
     * Writes a specified float {@code value}. The value is written into this buffer with four byte big endian.
     * @param value the number of float type
     */
    void writeFloat(float value);

    /**
     * Writes a specified double {@code value}. The value is written into this buffer with eight byte big endian.
     * @param value the number of double type
     */
    void writeDouble(double value);

    /**
     * Writes a specified long {@code value} with signed VBC.
     * @param value the number of long type
     */
    void writeVariableByteLong(long value);

    /**
     * Writes a specified int {@code value} with signed VBC.
     * @param value the number of int type
     */
    void writeVariableByteInteger(int value);

    /**
     * Writes null value with signed VBC.
     */
    void writeVariableByteNull();

    /**
     * Writes a specified {@code Integer value} with signed VBC.
     * @param value the number of {@code Integer}
     */
    void writeVariableByteInteger(Integer value);

    /**
     * Writes a specified {@code Long value} with signed VBC.
     * @param value the number of {@code Long}
     */
    void writeVariableByteLong(Long value);

    /**
     * Writes a specified string using a specified {@code charsetEncoder}.
     * If an {@code java.nio.charset.CharacterCodingException} happens, this method throws
     * {@code java.lang.RuntimeException} which has {@code CharacterCodingException} as its cause.
     *
     * @param charsetEncoder encoder to encode the given string {@code s}.
     * @param s string to be written
     */
    void writeString(CharsetEncoder charsetEncoder, String s);

    /**
     * Drains this buffer to a specified {@code encodeBuffer} instance. This buffer is read by
     * {@code encodeBuffer} and gets empty.
     *
     * @param encodeBuffer buffer which is drained to.
     */
    void drainTo(EncodeBuffer encodeBuffer);

    /**
     * Returns the size of written bytes into this buffer. This is equal to the current position.
     * @return the size of written bytes into this buffer
     */
    int  filledBytes();

    /**
     * Returns the limit of this buffer.
     * @return the limit of this buffer
     */
    int limitBytes();

    /**
     * Clears this buffer. The current position is set to 0, and
     */
    void clear();

    /**
     * Creates {@link net.ihiroky.niotty.buffer.BufferSink} from this buffer contents.
     * @return {@code BufferSink} containing this buffer contents
     */
    BufferSink createBufferSink();

    /**
     * Returns true if this buffer is backed by a byte array.
     * @return true if this buffer is backed by a byte array
     */
    boolean hasArray();

    /**
     * Returns a byte array that backs this buffer.
     * Modification to this buffer's content modifies the byte array, and vice versa as long as this buffer does not
     * expands its size. If {@link #hasArray()} returns false, this method throws
     * {@code java.lang.UnsupportedOperationException}.
     *
     * @return a byte array that backs this buffer
     * @throws UnsupportedOperationException if this buffer is not backed by a byte array
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
     * Returns a {@code ByteBuffer} that backs this buffer.
     * Modification to this buffer's content modifies the ByteBuffer, and vice versa.
     * @return a ByteBuffer that backs this buffer
     */
    ByteBuffer toByteBuffer();
}
