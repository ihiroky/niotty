package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;

/**
 * @author Hiroki Itoh
 */
public final class Buffers {

    private Buffers() {
        throw new AssertionError();
    }

    public static BufferSink createBufferSink(byte[] byteArray) {
        return new ArrayBufferSink(byteArray);
    }

    public static BufferSink createBufferSink(byte[] ...byteArrays) {
        int length = byteArrays.length;
        BufferSink[] s = new BufferSink[length];
        for (int i = 0; i < length; i++) {
            s[i] = new ArrayBufferSink(byteArrays[i]);
        }
        return new CompositeBufferSink(s);
    }

    public static DecodeBuffer createDecodeBuffer(ByteBuffer byteBuffer) {
        return new ByteBufferDecodeBuffer(byteBuffer);
    }
}
