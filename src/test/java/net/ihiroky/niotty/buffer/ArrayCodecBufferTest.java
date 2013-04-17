package net.ihiroky.niotty.buffer;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

/**
 * @author Hiroki Itoh
 */
@RunWith(Enclosed.class)
public class ArrayCodecBufferTest {

    public static class EmptyCase extends CodecBufferTestAbstract.AbstractEmptyTests {
        @Override
        protected CodecBuffer createCodecBuffer() {
            return new ArrayCodecBuffer();
        }
    }

    public static class ReadTests extends CodecBufferTestAbstract.AbstractReadTests {
        @Override
        protected CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length) {
            return new ArrayCodecBuffer(buffer, offset, length);
        }
    }

    public static class WriteTests extends CodecBufferTestAbstract.AbstractWriteTests {
        @Override
        protected CodecBuffer createDefaultCodecBuffer() {
            return new ArrayCodecBuffer();
        }

        @Override
        protected CodecBuffer createCodecBuffer(int initialCapacity) {
            return new ArrayCodecBuffer(initialCapacity);
        }
    }

    public static class BufferSinkTests extends CodecBufferTestAbstract.AbstractBufferSinkTests {
        @Override
        protected CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length) {
            return new ArrayCodecBuffer(buffer, offset, length);
        }
    }

    public static class StructureChangeTests extends CodecBufferTestAbstract.AbstractStructureChangeTests {
        @Override
        protected CodecBuffer createCodecBuffer(byte[] data, int offset, int length) {
            return new ArrayCodecBuffer(data, offset, length);
        }

        @Override
        protected CodecBuffer createDirectCodecBuffer(ByteBuffer directBuffer) {
            return new ByteBufferCodecBuffer(directBuffer);
        }
    }
}
