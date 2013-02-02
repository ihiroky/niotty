package net.ihiroky.niotty.buffer;

/**
 * Created on 13/02/01, 11:30
 *
 * @author Hiroki Itoh
 */
public interface EncodeBuffer {

    void writeByte(int value);
    void writeBytes(byte[] bytes, int offset, int length);
    void writeBytes4(int bits, int bytes);
    void writeBytes8(long bits, int bytes);
    void writeShort(short value);
    void writeChar(char value);
    void writeInt(int value);
    void writeLong(long value);
    void writeFloat(float value);
    void writeDouble(double value);

    int  filledBytes();
    void clear();


}
