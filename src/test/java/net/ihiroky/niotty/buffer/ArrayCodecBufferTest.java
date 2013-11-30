package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

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

        @Test
        public void testSpaceBytes() throws Exception {
            CodecBuffer sut = new ArrayCodecBuffer();
            assertThat(sut.space(), is(512));
        }
        @Test
        public void testCapacityBytes() throws Exception {
            CodecBuffer sut = new ArrayCodecBuffer();
            assertThat(sut.capacity(), is(512));
        }
    }

    public static class ReadTests extends CodecBufferTestAbstract.AbstractReadTests {
        @Override
        protected CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length) {
            return new ArrayCodecBuffer(buffer, offset, length);
        }
    }

    public static class UnsignedReadTests extends CodecBufferTestAbstract.AbstractUnsignedTest {
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
            return new ArrayCodecBuffer(ArrayChunkFactory.instance(), initialCapacity);
        }
    }

    public static class PacketTests extends CodecBufferTestAbstract.AbstractPacketTests {
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
    }

    public static class ReferenceCountTests {

        private ArrayCodecBuffer sut_;

        @Before
        public void setUp() {
            byte[] data = new byte[10];
            Arrays.fill(data, (byte) 1);
            sut_ = new ArrayCodecBuffer(ArrayChunkFactory.instance(), 10);
            sut_.writeBytes(data, 0, data.length);
        }

        @Test
        public void testExpand() throws Exception {
            byte[] data = new byte[3];

            int beforeCapacity = sut_.capacity();
            int beforeReferenceCount = sut_.chunk().referenceCount();
            sut_.writeBytes(data, 0, data.length);
            int afterCapacity = sut_.capacity();
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
