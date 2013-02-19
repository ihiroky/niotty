package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;

/**
 *
 * @author Hiroki Itoh
 */
public final class Buffers {

    // TODO must be read only
    private static final EncodeBuffer NULL_ENCODE_BUFFER = new ArrayEncodeBuffer(0);

    private Buffers() {
        throw new AssertionError();
    }

    static int outputByteBufferSize(float bytesPerChar, int chars) {
        return (int) (bytesPerChar * chars) + 1;
    }

    static int outputCharBufferSize(float charsPerByte, int bytes) {
        return (int) (charsPerByte * bytes) + 1;
    }

    static CharBuffer expand(CharBuffer original, float charsPerByte, int remainingBytes) {
        CharBuffer t = CharBuffer.allocate(
                original.capacity() + Buffers.outputCharBufferSize(charsPerByte, remainingBytes));
        original.flip();
        t.put(original);
        return t;
    }

    static void throwRuntimeException(CoderResult coderResult) {
        if (coderResult.isMalformed()) {
            throw new RuntimeException(new MalformedInputException(coderResult.length()));
        } else if (coderResult.isUnmappable()) {
            throw new RuntimeException(new UnmappableCharacterException(coderResult.length()));
        }
    }

    public static EncodeBuffer emptyEncodeBuffer() {
        return NULL_ENCODE_BUFFER;
    }

    public static EncodeBuffer newEncodeBuffer() {
        return new ArrayEncodeBuffer();
    }

    public static EncodeBuffer newEncodeBuffer(int initialCapacity) {
        return new ArrayEncodeBuffer(initialCapacity);
    }

    public static EncodeBuffer newEncodeBuffer(byte[] buffer, int offset, int length) {
        return ArrayEncodeBuffer.wrap(buffer, offset, length);
    }

    public static DecodeBuffer newDecodeBuffer(ByteBuffer byteBuffer) {
        return ByteBufferDecodeBuffer.wrap(byteBuffer);
    }

    public static BufferSink createBufferSink(BufferSink header, BufferSink body) {
        return new CompositeBufferSink(header, body);
    }
}
