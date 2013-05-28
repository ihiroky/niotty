package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

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

        @Test
        public void testSpaceBytes() throws Exception {
            CodecBuffer sut = new ByteBufferCodecBuffer();
            assertThat(sut.spaceBytes(), is(512));
        }
        @Test
        public void testCapacityBytes() throws Exception {
            CodecBuffer sut = new ByteBufferCodecBuffer();
            assertThat(sut.capacityBytes(), is(512));
        }

    }

    public static class ReadTests extends CodecBufferTestAbstract.AbstractReadTests {
        @Override
        protected CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length) {
            return Buffers.wrap(ByteBuffer.wrap(buffer, offset, length));
        }
    }

    public static class WriteTests extends CodecBufferTestAbstract.AbstractWriteTests {
        @Override
        protected CodecBuffer createDefaultCodecBuffer() {
            return new ByteBufferCodecBuffer();
        }

        @Override
        protected CodecBuffer createCodecBuffer(int initialCapacity) {
            return new ByteBufferCodecBuffer(ByteBufferChunkFactory.heap(), initialCapacity);
        }
    }

    public static class BufferSinkTests extends CodecBufferTestAbstract.AbstractBufferSinkTests {
        @Override
        protected CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length) {
            return Buffers.wrap(ByteBuffer.wrap(buffer, offset, length));
        }
    }

    public static class StructureChangeTests extends CodecBufferTestAbstract.AbstractStructureChangeTests {

        @Override
        protected CodecBuffer createCodecBuffer(byte[] data, int offset, int length) {
            return Buffers.wrap(ByteBuffer.wrap(data, offset, length));
        }
    }

    public static class ReferenceCountTests {

        private ByteBufferCodecBuffer sut_;

        @Before
        public void setUp() {
            byte[] data = new byte[10];
            Arrays.fill(data, (byte) 1);
            sut_ = new ByteBufferCodecBuffer(ByteBufferChunkFactory.heap(), 10);
            sut_.writeBytes(data, 0, data.length);
        }

        @Test
        public void testExpand() throws Exception {
            byte[] data = new byte[3];

            int beforeCapacity = sut_.capacityBytes();
            int beforeReferenceCount = sut_.chunk().referenceCount();
            sut_.writeBytes(data, 0, data.length);
            int afterCapacity = sut_.capacityBytes();
            int afterReferenceCount = sut_.chunk().referenceCount();
            sut_.dispose();
            int lastReferenceCount = sut_.chunk().referenceCount();

            assertThat(beforeCapacity, is(10));
            assertThat(beforeReferenceCount, is(1));
            assertThat(afterCapacity, is(20));
            assertThat(afterReferenceCount, is(1));
            assertThat(lastReferenceCount, is(0));
        }

        @Test
        public void testSlice_All() throws Exception {
            int first = sut_.chunk().referenceCount();
            CodecBuffer sliced = sut_.slice();
            int afterSliced = sut_.chunk().referenceCount();
            sut_.dispose();
            int afterSutDisposed = sut_.chunk().referenceCount();
            sliced.dispose();
            int afterSlicedDisposed = sut_.chunk().referenceCount();

            assertThat(first, is(1));
            assertThat(afterSliced, is(2));
            assertThat(afterSutDisposed, is(1));
            assertThat(afterSlicedDisposed, is(0));
        }

        @Test
        public void testSlice_Part() throws Exception {
            int first = sut_.chunk().referenceCount();
            CodecBuffer sliced = sut_.slice(3);
            int afterSliced = sut_.chunk().referenceCount();
            sut_.dispose();
            int afterSutDisposed = sut_.chunk().referenceCount();
            sliced.dispose();
            int afterSlicedDisposed = sut_.chunk().referenceCount();

            assertThat(first, is(1));
            assertThat(afterSliced, is(2));
            assertThat(afterSutDisposed, is(1));
            assertThat(afterSlicedDisposed, is(0));
        }

        @Test
        public void testDuplicate() throws Exception {
            int first = sut_.chunk().referenceCount();
            CodecBuffer duplicated = sut_.duplicate();
            int afterDuplicated = sut_.chunk().referenceCount();
            sut_.dispose();
            int afterSutDisposed = sut_.chunk().referenceCount();
            duplicated.dispose();
            int afterDuplicatedDisposed = sut_.chunk().referenceCount();

            assertThat(first, is(1));
            assertThat(afterDuplicated, is(2));
            assertThat(afterSutDisposed, is(1));
            assertThat(afterDuplicatedDisposed, is(0));
        }
    }
}
