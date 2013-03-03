package net.ihiroky.niotty.buffer;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

/**
 * @author Hiroki Itoh
 */
@RunWith(Enclosed.class)
public class ByteBufferCodecBufferTest {

    public static class EmptyTests extends CodecBufferTestAbstract.AbstractEmptyTests {
        @Override
        protected CodecBuffer createCodecBuffer() {
            return new ByteBufferCodecBuffer();
        }
    }

    public static class ReadTests extends CodecBufferTestAbstract.AbstractReadTests {
        @Override
        protected CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length) {
            return new ByteBufferCodecBuffer(ByteBuffer.wrap(buffer, offset, length));
        }
    }

    public static class WriteTests extends CodecBufferTestAbstract.AbstractWriteTests {
        @Override
        protected CodecBuffer createDefaultCodecBuffer() {
            return new ByteBufferCodecBuffer();
        }

        @Override
        protected CodecBuffer createCodecBuffer(int initialCapacity) {
            return new ByteBufferCodecBuffer(initialCapacity);
        }
    }

    public static class BufferSinkTests extends CodecBufferTestAbstract.AbstractBufferSinkTests {
        @Override
        protected CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length) {
            return new ByteBufferCodecBuffer(ByteBuffer.wrap(buffer, offset, length));
        }
    }
}
