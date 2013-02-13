package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;

/**
 *
 * @author Hiroki Itoh
 */
public final class Buffers {

    private Buffers() {
        throw new AssertionError();
    }

    // TODO must be read only
    private static final EncodeBuffer NULL_ENCODE_BUFFER = new ArrayEncodeBuffer(0);

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
