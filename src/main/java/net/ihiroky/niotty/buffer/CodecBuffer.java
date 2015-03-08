package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * A buffer class for encoding and decoding byte array. Implementations of this class has start, end and capacity
 * for internal storage. The start shows a start index of the content in the storage.
 * The end is the end index of the contents in the storage; the end is not a part of the content, next to the last
 * index of content. The capacity is the length of the storage capacity.
 * <p></p>
 * The start and end is 0 when new {@code CodecBuffer} is instantiated. Some data is written into the
 * {@code CodecBuffer}, then end increase at the written data size. By contrast, Some data is read from the
 * {@code CodecBuffer}, then start increase at the read data size. So remaining (readable) data size is calculated
 * by a difference between the start and the end, and space (writable) data size is calculated by the a difference
 * between the end and the capacity.
 * <p></p>
 * {@code CodecBuffer} supports primitive and string write and read operations. And supports a signed integer
 * encoding with variable length, signed VBC (Variable Byte Codes). The encoding has an end bit,
 * a sign bit and data bits. Each MSB per byte is the end bit. The end bit in last byte is 1, otherwise 0.
 * The sign bit is second bit from MSB in the first byte. If a value to be encoded is positive or zero,
 * then its sign bit is 0. If negative, then 1. The other bits are the data bits with little endian.
 * The data bits in first byte is 6 bits, and in the other byte is 7 bits. The data in the data bits is its magnitude,
 * not complement of two. This encoding support {@code null} value as negative zero; data bits show zero
 * and the sign bit and the end bit is 1 in single byte. The example of encoding -229 as binary format is
 * {@code 01100101 10000011}; the second bit from MSB in first byte is 1 for negative,
 * the MSB the second byte is 1 for its last byte, and the data bits in first byte shows 37
 * and in second byte shows 192 (128 + 64). Unsigned integer encoding with variable length is known as
 * Variable Byte Codes (<a href="http://nlp.stanford.edu/IR-book/html/htmledition/variable-byte-codes-1.html"></a>).
 * <p></p>
 *
 * <p></p>
 * The storage is automatically expanded if the end exceeds the capacity on write operation.
 * <p></p>
 */
public interface CodecBuffer extends Packet {

    @Override
    CodecBuffer addFirst(CodecBuffer buffer);

    @Override
    CodecBuffer addLast(CodecBuffer buffer);

    /**
     * Writes a {@code value} as byte to the end index.
     * Upper three bytes of the value is ignored.
     *
     * @param value the value
     * @return this object
     */
    CodecBuffer writeByte(int value);

    /**
     * Writes a specified byte array from the end index.
     *
     * @param bytes the byte array to be written
     * @param offset first position in the {@code byte} to be written from;
     *               must be non-negative and less than {@code bytes.length}
     * @param length byte size to be written from {@code offset};
     *               must be non-negative and less than or equal to {@code bytes.length - offset}
     */
    CodecBuffer writeBytes(byte[] bytes, int offset, int length);

    /**
     * Writes a specified {@code java.nio.ByteBuffer} from the end index.
     *
     * @param byteBuffer the byte buffer to be written
     * @return this object
     */
    CodecBuffer writeBytes(ByteBuffer byteBuffer);

    /**
     * Writes a value as short from the end index.
     * Upper two bytes of the value is ignored.
     *
     * @param value the value
     * @return this object
     */
    CodecBuffer writeShort(int value);

    /**
     * Writes a char {@code value} from the end index.
     *
     * @param value the value
     * @return this object
     */
    CodecBuffer writeChar(char value);

    /**
     * Writes a value as three bytes int from the end index.
     * Upper one bytes of the value is ignored.
     *
     * @param value the value
     * @return this object
     */
    CodecBuffer writeMedium(int value);

    /**
     * Writes a value as int from the end index.
     *
     * @param value the number of int type
     * @return this object
     */
    CodecBuffer writeInt(int value);

    /**
     * Writes a value as long from the end index.
     *
     * @param value the number of long type
     * @return this object
     */
    CodecBuffer writeLong(long value);

    /**
     * Writes a specified float {@code value} from the end index.
     *
     * @param value the number of float type
     * @return this object
     */
    CodecBuffer writeFloat(float value);

    /**
     * Writes a specified double {@code value} from the end index.
     *
     * @param value the number of double type
     * @return this object
     */
    CodecBuffer writeDouble(double value);

    /**
     * Writes a specified long {@code value} with signed VBC from the end index.
     *
     * @param value the number of long type
     * @return this object
     */
    CodecBuffer writeVariableByteLong(long value);

    /**
     * Writes a specified int {@code value} with signed VBC from the end index.
     *
     * @param value the number of int type
     * @return this object
     */
    CodecBuffer writeVariableByteInteger(int value);

    /**
     * Writes null value with signed VBC from the end index.
     *
     * @return this object
     */
    CodecBuffer writeVariableByteNull();

    /**
     * Writes a specified {@code Integer value} with signed VBC from the end index.
     *
     * @param value the number of {@code Integer}
     * @return this object
     */
    CodecBuffer writeVariableByteInteger(Integer value);

    /**
     * Writes a specified {@code Long value} with signed VBC from the end index.
     * @param value the number of {@code Long}
     */
    CodecBuffer writeVariableByteLong(Long value);

    /**
     * Writes a specified string as bytes and its length with a specified {@code encoder} from the end index.
     *
     * @param s string to be written
     * @param encoder encoder to convert the string {@code s} to bytes written into this buffer
     * @return this object
     */
    CodecBuffer writeString(String s, CharsetEncoder encoder);

    /**
     * Writes a specified string as bytes with a specified {@code encoder} from the end index.
     *
     * @param s string to be written
     * @param encoder encoder to convert the string {@code s} to bytes written into this buffer
     * @return this object
     */

    CodecBuffer writeStringContent(String s, CharsetEncoder encoder);

    /**
     * Writes a specified long value as an ascii string (byte array of ascii code) from the end index.
     * @param value the long value
     * @return this object
     */
    CodecBuffer writeLongAsAscii(long value);

    /**
     * Reads a byte from the start index.
     *
     * @return the byte
     * @throws java.lang.RuntimeException if the start exceeds the end index
     */
    byte readByte();

    /**
     * Reads a unsigned byte from the start index.
     *
     * @return the value
     * @throws java.lang.RuntimeException if the start exceeds the end index
     */
    int readUnsignedByte();

    /**
     * Reads bytes from the index into the specified {@code bytes}.
     *
     * @param bytes a byte array into which is data is written
     * @param offset first index in {@code bytes} which is written from;
     *               must be non-negative and less than or equal to {@code length}
     * @param length maximum number of bytes to be written into the {@code bytes};
     *               must be non-negative and less than or equal to {@code bytes.length - offset}
     * @return the total number of byte read into the {@code bytes}
     */
    int readBytes(byte[] bytes, int offset, int length);

    /**
     * Reads bytes from the start index into the specified {@code byteBuffer}.
     *
     * If the {@code byteBuffer} has enough space to read the whole buffer data, the {@code byteBuffer} gets
     * all. If not, the {@code byteBuffer} is filled with the part of this buffer data.
     *
     * @param byteBuffer a {@code ByteBuffer} into which is data is written
     * @return the total number of byte read into the {@code bytes}
     */
    int readBytes(ByteBuffer byteBuffer);

    /**
     * Reads char value from the start index.
     *
     * @return the value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of char
     */
    char readChar();

    /**
     * Reads short value from the start index.
     *
     * @return the value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of short
     */
    short readShort();

    /**
     * Reads unsigned short value from the start index.
     *
     * @return the value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of short
     */
    int readUnsignedShort();

    /**
     * Reads three bytes int value from the start index.
     *
     * @return int value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of int
     */
    int readMedium();

    /**
     * Reads unsigned three bytes int value from the start index.
     *
     * @return the value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of int
     */
    int readUnsignedMedium();

    /**
     * Reads int value from the start index.
     *
     * @return the value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of int
     */
    int readInt();

    /**
     * Reads unsigned int value from the start index.
     *
     * @return the value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of int
     */
    long readUnsignedInt();

    /**
     * Reads long value from the start index.
     *
     * @return the value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of long
     */
    long readLong();

    /**
     * Reads float value from the start index.
     *
     * @return the float value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of float
     */
    float readFloat();

    /**
     * Reads double value from the start index.
     *
     * @return the double value read from the buffer
     * @throws java.lang.RuntimeException if the remaining data in the buffer is less than the size of double
     */
    double readDouble();

    /**
     * Reads {@code Integer or Long} value in signed VBC form from the start index. The result may be null.
     * @return the {@code Integer or Long} value read from the buffer
     */
    Number readVariableByteNumber();

    /**
     * Reads {@code long} value in signed VBC form from the start index.
     * The null value is returned as (negative) zero.
     * @return the {@code long} value read from the buffer
     */
    long readVariableByteLong();

    /**
     * Reads {@code int} value in signed VBC form from the start index.
     * The null value is returned as (negative) zero.
     * @return the {@code int} value read from the buffer
     */
    int readVariableByteInteger();

    /**
     * Reads {@code String} value written by {@link #writeString(String, java.nio.charset.CharsetEncoder)}.
     *
     * @param decoder the decoder of the charset which creates encoder of
     *                {@link #writeString(String, java.nio.charset.CharsetEncoder)}
     * @return the {@code String} value read from the buffer
     */
    String readString(CharsetDecoder decoder);

    /**
     * Reads bytes as string from the start index using a specified {@code decoder}.
     * The string length of encoded byte format is given as {@code length}. If some
     * {@code java.nio.charset.CharacterCodingException} happens, this method throws
     * {@code java.lang.RuntimeException} which has {@code CharacterCodingException} as its cause.
     *
     * @param decoder the decoder to convert the buffer to a string.
     * @param length the length of byte data to be decoded by {@code decoder}
     * @return the string value read from the buffer
     */
    String readStringContent(CharsetDecoder decoder, int length);

    /**
     * Reads a long value from the start index, assumes that the content is
     * the number expression of an ascii string. The string length of the content
     * is given as {@code length}.
     * {@code java.nio.charset.CharacterCodingException} happens, this method throws
     * {@code java.lang.RuntimeException} which has {@code CharacterCodingException} as its cause.
     *
     * @param length the length of the content
     * @return the long value
     */
    long readLongAsAscii(int length);

    /**
     * Add {@code n} to the start index.
     * <p/>
     * If the start index is out of [0, end]), the actual number of bytes to be skipped is adjusted
     * to be fit into the range.
     *
     * @param n the number of bytes to be skipped
     * @return the actual number of bytes skipped
     */
    int skipStartIndex(int n);

    /**
     * Add {@code n} to the end index.
     * <p/>
     * If the end index is out of [start, {@link #capacity()}]), the actual number of bytes to be skipped is adjusted
     * to be fit into the range.
     *
     * @param n the number of bytes to be skipped
     * @return the actual number of bytes skipped
     */
    int skipEndIndex(int n);

    /**
     * Returns the size of remaining data by the byte.
     * <p/>
     * This is equals to difference between the start and the end index.
     * @return the size of remaining data by the byte
     */
    int remaining();

    /**
     * Returns the size of space to be written data by the byte.
     * <p/>
     * @return the size of space to be written data by the byte
     */
    int space();

    /**
     * Returns the capacity of this buffer by the byte.
     * @return the capacity of this buffer by the byte
     */
    int capacity();

    /**
     * Returns the value of the start index.
     * @return the value of the start index
     */
    int startIndex();

    /**
     * Sets the value of the start index.
     * @param start the value to be set
     * @return this {@code CodecBuffer}
     * @throws java.lang.IndexOutOfBoundsException if {@code start} is out of range
     */
    CodecBuffer startIndex(int start);

    /**
     * Returns the value of the end index.
     * @return the value of the end index
     */
    int endIndex();

    /**
     * Sets the value of the end index.
     * @param end the value to be set
     * @return this {@code CodecBuffer}
     * @throws java.lang.IndexOutOfBoundsException if {@code end} is out of range
     */
    CodecBuffer endIndex(int end);

    /**
     * Drains from a specified {@code buffer}'s contents to this instance. The {@code decodeBuffer} is read
     * by this instance and gets empty.
     *
     * @param buffer buffer which is drained from.
     * @return the number of byte to be transferred
     */
    int drainFrom(CodecBuffer buffer);

    /**
     * Drains from a specified {@code buffer}'s contents to this instance at most specified {@code bytes}.
     *
     * @param buffer buffer which is drained from.
     * @param bytes the number by byte to be transferred
     * @return the number of bytes transferred
     */
    int drainFrom(CodecBuffer buffer, int bytes);

    @Override
    CodecBuffer slice(int bytes);

    /**
     * Creates a new buffer whose content is a shared subsequence of this buffer's content.
     * The content of the new buffer will start at this buffer's current start index.
     * Changes to this buffer's content will be visible in the new buffer, and vice versa;
     * the two buffers' start index and end index will be independent. The new buffer's start index
     * will be zero, its capacity and its end index will be the number of bytes remaining in this buffer.
     *
     * @return the new buffer
     */
    CodecBuffer slice();

    /**
     * Creates a new buffer that shares this buffer's content.
     * The content of the new buffer will be that of this buffer. Changes to this buffer's content will be visible
     * in the new buffer, and vice versa; the two buffers' start index and end index values will be independent.
     * The new buffer's capacity, start index and end index values will be identical to those of this buffer.
     *
     * @return the new buffer
     */
    CodecBuffer duplicate();

    /**
     * <p>Compacts this buffer.</p>
     *
     * <p>The bytes between the buffer's current start index {@code s} and end index {@code e}
     * are copied to the head of the region in this buffer. The byte at {@code s} is copied to index zero,
     * the byte at index {@code s + 1} is copied to index one, and so forth until the byte
     * at index {@code e - 1} is copied to index {@code e - s - 1}. The {@code s} is then
     * set to {@code 0} and the {@code e} is set to {@code e - s}.</p>
     * @return this {@code CodecBuffer}
     */
    CodecBuffer compact();

    /**
     * Clears this buffer. The the start index and the end index is set to 0.
     * @return this {@code CodecBuffer}
     */
    CodecBuffer clear();

    /**
     * Converts remaining buffer contents to {@code java.nio.ByteBuffer}.
     * An internal storage in this buffer is shared with the result {@code ByteBuffer}. If some data is written into
     * the {@code ByteBuffer}, then this buffer is also modified and vice versa.
     * @return {@code java.nio.ByteBuffer}
     */
    ByteBuffer byteBuffer();

    @Override
    boolean hasArray();

    @Override
    byte[] array();

    @Override
    int arrayOffset();

    /**
     * Returns the index within this buffer of the first occurrence of a specified byte,
     * starting the search at a specified index and to the ascending direction.
     * There is no restriction on the value of fromIndex. If it is negative, it has the same effect
     * as if it were zero: this entire buffer may be searched. If it is greater than the remaining of this buffer,
     * it has the same effect as if it were equal to the length of this buffer: -1 is returned.
     *
     * @param b the byte to be searched
     * @param fromIndex the index to start the search from
     * @return the index of the first occurrence of the {@code b} in this buffer that is greater than
     * or equal to {@code fromIndex}, or -1 if the {@code b} does not occur.
     */
    int indexOf(int b, int fromIndex);

    /**
     * Returns the index within this buffer of the first occurrence of specified bytes,
     * starting the search at a specified index and to the ascending direction. The index of the start index is 0.
     * There is no restriction on the value of fromIndex. If it is negative, it has the same effect
     * as if it were zero: this entire buffer may be searched. If it is greater than the remaining of this buffer,
     * it has the same effect as if it were equal to the length of this buffer: -1 is returned.
     *
     * @param b the bytes to be searched
     * @param fromIndex the index to start the search from
     * @return the index of the first occurrence of the {@code b} in this buffer that is greater than
     * or equal to {@code fromIndex}, or -1 if the {@code b} does not occur.
     */
    int indexOf(byte[] b, int fromIndex);

    /**
     * Returns the index within this buffer of the first occurrence of a specified byte,
     * starting the search at a specified index and to the descending direction. The index of the start index is 0.
     * There is no restriction on the value of fromIndex. If it is greater than or equal to the remaining
     * of this buffer, it has the same effect as if it were equal to one less than the remaining of this buffer:
     * this entire buffer may be searched. If it is negative, it has the same effect as if it were -1: -1 is returned.
     *
     * @param b the byte to be searched
     * @param fromIndex the index to start the search from
     * @return the index of the first occurrence of the {@code b} in this buffer that is greater than
     * or equal to {@code fromIndex}, or -1 if the {@code b} does not occur.
     */
    int lastIndexOf(int b, int fromIndex);

    /**
     * Returns the index within this buffer of the first occurrence of specified bytes,
     * starting the search at a specified index and to the descending direction. The index of the start index is 0.
     * There is no restriction on the value of fromIndex. If it is greater than or equal to the remaining
     * of this buffer, it has the same effect as if it were equal to one less than the remaining of this buffer:
     * this entire buffer may be searched. If it is negative, it has the same effect as if it were -1: -1 is returned.
     *
     * @param b the bytes to be searched
     * @param fromIndex the index to start the search from
     * @return the index of the first occurrence of the {@code b} in this buffer that is greater than
     * or equal to {@code fromIndex}, or -1 if the {@code b} does not occur.
     */
    int lastIndexOf(byte[] b, int fromIndex);
}
