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

    public static DecodeBuffer newDecodeBuffer(ByteBuffer byteBuffer) {
        return ByteBufferDecodeBuffer.wrap(byteBuffer);
    }

    public static BufferSink createBufferSink(byte[] byteArray) {
        return new ArrayBufferSink(byteArray, 0, byteArray.length);
    }

    public static BufferSink createBufferSink(byte[] ...byteArrays) {
        int length = byteArrays.length;
        BufferSink[] s = new BufferSink[length];
        for (int i = 0; i < length; i++) {
            s[i] = new SegmentedArrayBufferSink(byteArrays[i]);
        }
        return new CompositeBufferSink(s);
    }
}
