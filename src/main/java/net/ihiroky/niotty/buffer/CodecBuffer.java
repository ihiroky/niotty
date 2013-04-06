package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * A buffer class for encoding and decoding byte array. Implementations of this class has beginning, end and capacity
 * for internal storage. The beginning shows a start index of the content in the storage.
 * The end is the end index of the contents in the storage; the end is not a part of the content, next to the last
 * index of content. The capacity is the length of the storage capacity.
 * <p></p>
 * The beginning and end is 0 when new {@code CodecBuffer} is instantiated. Some data is written into the
 * {@code CodecBuffer}, then end increase at the written data size. By contrast, Some data is read from the
 * {@code CodecBuffer}, then beginning increase at the read data size. So remaining (readable) data size is calculated
 * by a difference between the beginning and the end, and space (writable) data size is calculated by the a difference
 * between the end and the capacity.
 * <p></p>
 * Almost read and write methods are relative operation; the beginning and the end are changed by the operation.
 * But {@link #readByte(int)} and {@link #writeByte(int, int)} are absolute operation; the beginning and
 * the end are not changed.
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
 * @author Hiroki Itoh
 */
public interface CodecBuffer extends BufferSink {

    /**
     * Writes a {@code value} as byte.
     *
     * @param value byte value
     */
    void writeByte(int value);

    /**
     * Writes a {@code value} as byte at a specified {@code position}.
     * The value of the end is not changed.
     *
     * @param position index to write the {@code value}
     * @param value byte value
     */
    void writeByte(int position, int value);

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
     * @param byteBuffer the byte buffer to be written
     */
    void writeBytes(ByteBuffer byteBuffer);

    /**
     * Writes a specified {@code bits}, which byte size is specified {@code bytes}. The {@code bits} is right-aligned,
     * and is written into this buffer with big endian.
     *
     * @param bits the set of bit to be written
     * @param bytes byte size of the {@code bits}; must be non-positive and less than or equal to 4
     */
    void writeBytes4(int bits, int bytes);

    /**
     * Writes a specified {@code bits}, which byte size is specified {@code bytes}. The {@code bits} is right-aligned,
     * and is written into this buffer with big endian.
     *
     * @param bits the set of bit to be written
     * @param bytes byte size of the {@code bits}; must be non-positive and less than or equal to 8
     */
    void writeBytes8(long bits, int bytes);

    /**
     * Writes a specified short {@code value}. The value is written into this buffer with two byte big endian.
     * @param value the number of short type
     */
    void writeShort(short value);

    /**
     * Writes a specified char {@code value}. The value is written into this buffer with two byte big endian.
     * @param value the number of char type
     */
    void writeChar(char value);

    /**
     * Writes a specified int {@code value}. The value is written into this buffer with four byte big endian.
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
     * At first, the encoded byte size is written using VBC, and then the encoded byte is written.
     * If an {@code java.nio.charset.CharacterCodingException} happens, this method throws
     * {@code java.lang.RuntimeException} which has {@code CharacterCodingException} as its cause.
     *
     * @param charsetEncoder encoder to encode the given string {@code s}.
     * @param s string to be written
     */
    void writeString(CharsetEncoder charsetEncoder, String s);

    /**
     * Reads a byte from the buffer.
     *
     * @param position index to read from
     * @return a byte.
     * @throws java.lang.RuntimeException if the beginning exceeds the end
     */
    int    readByte(int position);

    /**
     * Reads a byte from the buffer at a specified {@code position}.
     *
     * @return a byte.
     * @throws java.lang.RuntimeException if the beginning exceeds the end
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
     * @throws java.lang.RuntimeException if the beginning exceeds the end,
     *         or {@code offset} or {@code length} is invalid for the buffer
     */
    void   readBytes(byte[] bytes, int offset, int length);

    /**
     * Reads bytes from the buffer.
     * If the {@code byteBuffer} has enough space to read the whole buffer data, the {@code byteBuffer} gets
     * all. If not, the {@code byteBuffer} is filled with the part of this buffer data.
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
     * At first, the encoded byte size is read using VBC, and then the encoded byte is read. The encoded byte is
     * decoded by {@code charsetDecoder}. If some {@code java.nio.charset.CharacterCodingException} happens,
     * this method throws {@code java.lang.RuntimeException} which has {@code CharacterCodingException} as its cause.
     *
     * @param charsetDecoder decoder to decode byte data
     * @throws java.lang.RuntimeException if an error happens
     * @return string value read from the buffer
     */
    String readString(CharsetDecoder charsetDecoder);

    /**
     * Reads a string from the buffer using a specified {@code charsetDecoder}.
     * The string length of encoded byte format is given as {@code bytes}. If some
     * {@code java.nio.charset.CharacterCodingException} happens, this method throws
     * {@code java.lang.RuntimeException} which has {@code CharacterCodingException} as its cause.
     *
     * @param charsetDecoder decoder to decode byte data
     * @param bytes length of byte data to be decoded by {@code charsetDecoder}
     * @throws java.lang.RuntimeException if an error happens
     * @return string value read from the buffer
     */
    String readString(CharsetDecoder charsetDecoder, int bytes);

    /**
     * Skips to read by specified size of byte of this buffer.
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
     * Returns the size of space to be written data in byte.
     * @return the size of space to be written data in byte
     */
    int spaceBytes();

    /**
     * Returns the capacity of this buffer.
     * @return the capacity of this buffer
     */
    int capacityBytes();

    /**
     * Returns the value of the beginning.
     * @return the value of the beginning
     */
    int beginning();

    /**
     * Sets the value of the beginning.
     * @param beginning the value to be set
     * @return this {@code CodecBuffer}
     * @throws java.lang.IndexOutOfBoundsException if {@code beginning} is out of range
     */
    CodecBuffer beginning(int beginning);

    /**
     * Returns the value of the end.
     * @return the value of the end
     */
    int end();

    /**
     * Sets the value of the end.
     * @param end the value to be set
     * @return this {@code CodecBuffer}
     * @throws java.lang.IndexOutOfBoundsException if {@code end} is out of range
     */
    CodecBuffer end(int end);

    /**
     * Drains from a specified {@code decodeBuffer}'s contents to this instance. The {@code decodeBuffer} is read
     * by this instance and gets empty.
     *
     * @param buffer buffer which is drained from.
     * @return the number of byte to be transferred
     */
    int drainFrom(CodecBuffer buffer);

    /**
     * Drains from a specified {@code decodeBuffer}'s contents to this instance at most specified {@code bytes}.
     *
     * @param buffer buffer which is drained from.
     * @param bytes the number by byte to be transferred
     * @return the number of bytes transferred
     */
    int drainFrom(CodecBuffer buffer, int bytes);

    /**
     * Creates new {@code DecodeBuffer} that shares this buffer's base content.
     * The beginning of the new {@code DecodeBuffer} is the one of the this buffer.
     * The end of the new {@code DecodeBuffer} is the {@code beginning + bytes}.
     * The two {@code DecodeBuffer}'s beginning and end are independent.
     * After this method is called, the beginning of this buffer increases {@code bytes}.
     *
     * @param bytes size of content to slice
     * @throws IllegalArgumentException if {@code bytes} exceeds this buffer's remaining.
     * @return the new {@code DecodeBuffer}
     */
    CodecBuffer slice(int bytes);

    /**
     * Shrinks unusable region; from index 0 to the beginning. The byte at the beginning is copied into the index 0,
     * the byte at beginning + 1 and the end - 1 is copied into end - 1 - beginning.
     * @return this {@code CodecBuffer}
     */
    CodecBuffer compact();

    /**
     * Clears this buffer. The the beginning and the end is set to 0.
     * @return this {@code CodecBuffer}
     */
    CodecBuffer clear();

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
     * Returns the index within this buffer of the first occurrence of a specified byte,
     * starting the search at a specified index and to the ascending direction.
     * There is no restriction on the value of fromIndex. If it is negative, it has the same effect
     * as if it were zero: this entire buffer may be searched. If it is greater than the length of this buffer,
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
     * starting the search at a specified index and to the ascending direction.
     * There is no restriction on the value of fromIndex. If it is negative, it has the same effect
     * as if it were zero: this entire buffer may be searched. If it is greater than the length of this buffer,
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
     * starting the search at a specified index and to the descending direction .
     * There is no restriction on the value of fromIndex. If it is greater than or equal to the length of this buffer,
     * it has the same effect as if it were equal to one less than the length of this buffer:
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
     * starting the search at a specified index and to the descending direction.
     * There is no restriction on the value of fromIndex. If it is greater than or equal to the length of this buffer,
     * it has the same effect as if it were equal to one less than the length of this buffer:
     * this entire buffer may be searched. If it is negative, it has the same effect as if it were -1: -1 is returned.
     *
     * @param b the bytes to be searched
     * @param fromIndex the index to start the search from
     * @return the index of the first occurrence of the {@code b} in this buffer that is greater than
     * or equal to {@code fromIndex}, or -1 if the {@code b} does not occur.
     */
    int lastIndexOf(byte[] b, int fromIndex);
}
