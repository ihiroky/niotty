package net.ihiroky.niotty.buffer;

/**
 *
 * @author Hiroki Itoh
 */
public final class Buffers {

    private Buffers() {
        throw new AssertionError();
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
