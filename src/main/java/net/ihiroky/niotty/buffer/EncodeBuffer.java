package net.ihiroky.niotty.buffer;

/**
 * A buffer class for encoding byte array. Implementations of this class has position and capacity.
 * for internal storage. The position shows a current writing byte position in the storage.
 * The data written into this class exists between index 0 and position - 1. The capacity is the end of the storage
 * for valid operation. The storage is automatically expanded if the position exceeds the capacity for a writing
 * operation.
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
     * Write a specified byte array.
     *
     * @param bytes the byte array to be written
     * @param offset first position in the {@code byte} to be written from;
     *               must be non-negative and less than {@code bytes.length}
     * @param length byte size to be written from {@code offset};
     *               must be non-negative and less than or equal to {@code bytes.length - offset}
     */
    void writeBytes(byte[] bytes, int offset, int length);

    /**
     * Write a specified {@code bits}, which byte size is specified {@code bytes}. The {@code bits} is right-aligned.
     * The {@code bits} is written into this buffer with big endian.
     *
     * @param bits the set of bit to be written
     * @param bytes byte size of the {@code bits}; must be non-positive and less than or equal to 4
     */
    void writeBytes4(int bits, int bytes);

    /**
     * Write a specified {@code bits}, which byte size is specified {@code bytes}. The {@code bits} is right-aligned.
     * The {@code bits} is written into this buffer with big endian.
     *
     * @param bits the set of bit to be written
     * @param bytes byte size of the {@code bits}; must be non-positive and less than or equal to 8
     */
    void writeBytes8(long bits, int bytes);

    /**
     * Write a specified short {@code value}. The value si written into this buffer with two byte big endian.
     * @param value the number of short type
     */
    void writeShort(short value);

    /**
     * Write a specified char {@code value}. The value si written into this buffer with two byte big endian.
     * @param value the number of char type
     */
    void writeChar(char value);

    /**
     * Write a specified int {@code value}. The value si written into this buffer with four byte big endian.
     * @param value the number of int type
     */
    void writeInt(int value);

    /**
     * Write a specified log {@code value}. The value si written into this buffer with eight byte big endian.
     * @param value the number of long type
     */
    void writeLong(long value);

    /**
     * Write a specified float {@code value}. The value si written into this buffer with four byte big endian.
     * @param value the number of float type
     */
    void writeFloat(float value);

    /**
     * Write a specified double {@code value}. The value si written into this buffer with eight byte big endian.
     * @param value the number of double type
     */
    void writeDouble(double value);

    /**
     * Returns the size of written bytes into this buffer. This is equal to the current position.
     * @return the size of written bytes into this buffer
     */
    int  filledBytes();

    /**
     * Returns the capacity of this buffer.
     * @return the capacity of this buffer
     */
    int  capacityBytes();

    /**
     * Clears this buffer. The current position is set to 0, and
     */
    void clear();

    /**
     * Creates {@link net.ihiroky.niotty.buffer.BufferSink} from this buffer contents.
     * @return {@code BufferSink} containing this buffer contents
     */
    BufferSink createBufferSink();

}
