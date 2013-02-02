package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;

/**
 * Created on 13/02/01, 13:22
 *
 * @author Hiroki Itoh
 */
public interface DecodeBuffer {

    int    readByte();
    int    readBytes(byte[] bytes, int offset, int length);
    int    readBytes4(int bytes);
    long   readBytes8(int bytes);
    char   readChar();
    short  readShort();
    int    readInt();
    long   readLong();
    float  readFloat();
    double readDouble();

    int  leftBytes();
    int  wholeBytes();
    void clear();
    BufferSink toBufferSink();
    void transferFrom(ByteBuffer byteBuffer);
}
