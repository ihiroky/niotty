package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
@RunWith(Enclosed.class)
public class SlicedCodecBufferTest {

    public static class EmptyTests extends CodecBufferTestAbstract.AbstractEmptyTests {
        @Override
        protected CodecBuffer createCodecBuffer() {
            CodecBuffer b = new ArrayCodecBuffer();
            return new SlicedCodecBuffer(b);
        }
    }

    public static class ReadTests extends CodecBufferTestAbstract.AbstractReadTests {
        @Override
        protected CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length) {
            CodecBuffer b = new ArrayCodecBuffer(buffer, offset, length);
            return new SlicedCodecBuffer(b);
        }

        @Test
        public void testCompact_BasedOnDirectBuffer() throws Exception {
            byte[] data = new byte[16];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) i;
            }
            ByteBuffer directBuffer = ByteBuffer.allocateDirect(16);
            directBuffer.limit(0);
            CodecBuffer b = new ByteBufferCodecBuffer(directBuffer);
            b.writeBytes(data, 0, data.length);
            b.beginning(1); // The offset of sliced buffer sets to 1.
            CodecBuffer sut = new SlicedCodecBuffer(b);

            sut.beginning(3);
            sut.compact();

            assertThat(sut.beginning(), is(0));
            assertThat(sut.end(), is(12));
            byte[] expected = new byte[13];
            sut.readBytes(expected, 0, expected.length);
            assertThat(expected[0], is((byte) 4));
            assertThat(expected[1], is((byte) 5));
            assertThat(expected[10], is((byte) 14));
            assertThat(expected[11], is((byte) 15));
        }
    }

    public static class WriteTests {

        SlicedCodecBuffer sut_;

        @Rule
        public ExpectedException exceptionRule_ = ExpectedException.none();

        @Before
        public void setUp() {
            byte[] data = new byte[11];
            Arrays.fill(data, (byte) 1);
            CodecBuffer b = new ArrayCodecBuffer(data, 0, data.length);
            b.beginning(1);
            sut_ = new SlicedCodecBuffer(b);
        }

        @Test
        public void testWriteByte() throws Exception {
            sut_.end(8);
            sut_.writeByte(10);
            assertThat(sut_.array()[9], is((byte) 10));
            sut_.writeByte(20);
            assertThat(sut_.array()[10], is((byte) 20));
        }

        @Test
        public void testWriteByte_ExpandIsException() throws Exception {
            exceptionRule_.expect(IndexOutOfBoundsException.class);
            exceptionRule_.expectMessage("no space is left. required: 1, space: 0");
            sut_.writeByte(1);
        }

        @Test
        public void testWriteBytes_Whole() throws Exception {
            byte[] data = new byte[] {0, 1, 2};
            sut_.end(7);

            sut_.writeBytes(data, 0, 3);

            assertThat(sut_.remainingBytes(), is(10));
            byte[] actual = new byte[3];
            sut_.beginning(7);
            sut_.readBytes(actual, 0, actual.length);
            assertThat(actual, is(data));
        }

        @Test
        public void testWriteBytes_WholeExpandIsException() throws Exception {
            byte[] data = new byte[3];
            Arrays.fill(data, (byte) 1);
            sut_.end(8);
            exceptionRule_.expect(IndexOutOfBoundsException.class);
            exceptionRule_.expectMessage("no space is left. required: 3, space: 2");

            sut_.writeBytes(data, 0, 3);
        }

        @Test
        public void testWriteBytes_Head() throws Exception {
            byte[] b = new byte[] {'0', '1', '2'};
            sut_.end(8);

            sut_.writeBytes(b, 0, 2);

            assertThat(sut_.remainingBytes(), is(10));
            byte[] actual = new byte[2];
            sut_.beginning(8);
            sut_.readBytes(actual, 0, actual.length);
            assertThat(actual[0], is((byte) 0x30));
            assertThat(actual[1], is((byte) 0x31));
        }

        @Test
        public void testWriteBytes_HeadExpandIsException() throws Exception {
            byte[] b = new byte[3];
            sut_.end(9);
            exceptionRule_.expect(IndexOutOfBoundsException.class);
            exceptionRule_.expectMessage("no space is left. required: 2, space: 1");

            sut_.writeBytes(b, 0, 2);
        }

        @Test
        public void testWriteBytes_Tail() throws Exception {
            byte[] b = new byte[] {'0', '1', '2'};
            sut_.end(8);

            sut_.writeBytes(b, 1, 2);

            assertThat(sut_.remainingBytes(), is(10));
            byte[] actual = new byte[2];
            sut_.beginning(8);
            sut_.readBytes(actual, 0, actual.length);
            assertThat(actual[0], is((byte) 0x31));
            assertThat(actual[1], is((byte) 0x32));
        }

        @Test
        public void testWriteBytes_TailExpandIsException() throws Exception {
            byte[] b = new byte[3];
            sut_.end(9);
            exceptionRule_.expect(IndexOutOfBoundsException.class);
            exceptionRule_.expectMessage("no space is left. required: 2, space: 1");

            sut_.writeBytes(b, 1, 2);
        }

        @Test
        public void testWriteBytes_ByteBufferWhole() throws Exception {
            ByteBuffer bb = ByteBuffer.wrap(new byte[]{'0', '1', '2'});
            sut_.end(7);

            sut_.writeBytes(bb);

            assertThat(sut_.remainingBytes(), is(10));
            byte[] actual = new byte[3];
            sut_.beginning(7);
            sut_.readBytes(actual, 0, actual.length);
            assertThat(actual[0], is((byte) 0x30));
            assertThat(actual[1], is((byte) 0x31));
            assertThat(actual[2], is((byte) 0x32));
        }

        @Test
        public void testWriteBytesByte_BufferWholeExpandIsException() throws Exception {
            ByteBuffer bb = ByteBuffer.wrap(new byte[3]);
            sut_.end(8);
            exceptionRule_.expect(IndexOutOfBoundsException.class);
            exceptionRule_.expectMessage("no space is left. required: 3, space: 2");

            sut_.writeBytes(bb);
        }

        @Test
        public void testWriteShort() throws Exception {
            sut_.end(8);
            sut_.writeShort(Short.MAX_VALUE);
            byte[] actual = sut_.array();
            assertThat(actual[9], is((byte) 0x7F));
            assertThat(actual[10], is((byte) 0xFF));
            assertThat(sut_.remainingBytes(), is(10));
        }

        @Test
        public void testWriteShort_ExpansionIsException() throws Exception {
            sut_.end(9);
            exceptionRule_.expect(IndexOutOfBoundsException.class);
            exceptionRule_.expectMessage("no space is left. required: 2, space: 1");

            sut_.writeShort(Short.MAX_VALUE);
        }

        @Test
        public void testWriteChar() throws Exception {
            sut_.end(8);

            sut_.writeChar((char) (Character.MAX_VALUE / 2));

            byte[] b = sut_.array();
            assertThat(b[9], is((byte) 0x7F));
            assertThat(b[10], is((byte) 0xFF));
            assertThat(sut_.remainingBytes(), is(10));
        }

        @Test
        public void testWriteInt() throws Exception {
            sut_.end(6);

            sut_.writeInt(Integer.MAX_VALUE);

            byte[] b = sut_.array();
            assertThat(b[7], is((byte) 0x7F));
            assertThat(b[8], is((byte) 0xFF));
            assertThat(b[9], is((byte) 0xFF));
            assertThat(b[10], is((byte) 0xFF));
            assertThat(sut_.remainingBytes(), is(10));
        }

        @Test
        public void testWriteInt_ExpansionIsException() throws Exception {
            sut_.end(7);
            exceptionRule_.expect(IndexOutOfBoundsException.class);
            exceptionRule_.expectMessage("no space is left. required: 4, space: 3");

            sut_.writeInt(Integer.MAX_VALUE);
        }

        @Test
        public void testWriteLong() throws Exception {
            sut_.end(2);

            sut_.writeLong(Long.MAX_VALUE);

            byte[] b = sut_.array();
            assertThat(b[3], is((byte) 0x7F));
            assertThat(b[4], is((byte) 0xFF));
            assertThat(b[5], is((byte) 0xFF));
            assertThat(b[6], is((byte) 0xFF));
            assertThat(b[7], is((byte) 0xFF));
            assertThat(b[8], is((byte) 0xFF));
            assertThat(b[9], is((byte) 0xFF));
            assertThat(b[10], is((byte) 0xFF));
            assertThat(sut_.remainingBytes(), is(10));
        }

        @Test
        public void testWriteLong_ExpansionIsException() throws Exception {
            sut_.end(3);
            exceptionRule_.expect(IndexOutOfBoundsException.class);
            exceptionRule_.expectMessage("no space is left. required: 8, space: 7");

            sut_.writeLong(Long.MAX_VALUE);
        }
        @Test
        public void testWriteFloat() throws Exception {
            sut_.end(0);
            sut_.writeFloat(1f);
            byte[] b = sut_.array();
            assertThat(b[1], is((byte) 0x3F));
            assertThat(b[2], is((byte) 0x80));
            assertThat(b[3], is((byte) 0x00));
            assertThat(b[4], is((byte) 0x00));
            assertThat(sut_.remainingBytes(), is(4));
        }

        @Test
        public void testWriteDouble() throws Exception {
            sut_.end(0);
            sut_.writeDouble(1d);
            byte[] b = sut_.array();
            assertThat(b[1], is((byte) 0x3F));
            assertThat(b[2], is((byte) 0xF0));
            assertThat(b[3], is((byte) 0x00));
            assertThat(b[4], is((byte) 0x00));
            assertThat(b[5], is((byte) 0x00));
            assertThat(b[6], is((byte) 0x00));
            assertThat(b[7], is((byte) 0x00));
            assertThat(b[8], is((byte) 0x00));
            assertThat(sut_.remainingBytes(), is(8));
        }

        @Test
        public void testArrayOffset() throws Exception {
            assertThat(sut_.arrayOffset(), is(1));
        }

        @Test
        public void testDrainFrom() throws Exception {
            byte[] a = new byte[10];
            Arrays.fill(a, (byte) 'a');
            sut_.clear();

            int drained = sut_.drainFrom(new ArrayCodecBuffer(a, 0, a.length));

            assertThat(drained, is(10));
            assertThat(sut_.remainingBytes(), is(10));
            assertThat(sut_.capacityBytes(), is(10));

            byte[] ea = new byte[a.length];
            sut_.readBytes(ea, 0, ea.length);
            assertThat(ea, is(a));
            assertThat(sut_.remainingBytes(), is(0));
        }

        @Test
        public void testDrainFrom_ExpansionIsException() throws Exception {
            byte[] a = new byte[11];
            Arrays.fill(a,(byte) 1);
            sut_.clear();
            exceptionRule_.expect(IndexOutOfBoundsException.class);
            exceptionRule_.expectMessage("no space is left. required: 11, space: 10");

            sut_.drainFrom(new ArrayCodecBuffer(a, 0, a.length));
        }

        @Test
        public void testDrainFrom_WithLimit() throws Exception {
            byte[] a = new byte[9];
            Arrays.fill(a, (byte) 'a');
            CodecBuffer input = new ArrayCodecBuffer(a, 0, a.length);
            sut_.clear();

            // drain 5 bytes.
            int drained = sut_.drainFrom(input, 6);
            assertThat(drained, is(6));
            assertThat(sut_.remainingBytes(), is(6));
            assertThat(sut_.spaceBytes(), is(4));
            assertThat(input.remainingBytes(), is(3));

            // try to drain 4 bytes, but drain left 3 bytes
            drained = sut_.drainFrom(input, 4);
            assertThat(drained, is(3));
            assertThat(sut_.remainingBytes(), is(9));
            assertThat(sut_.spaceBytes(), is(1));
            assertThat(input.remainingBytes(), is(0));
        }

        @Test
        public void testDrainFrom_WithLimitExpansionIsException() throws Exception {
            exceptionRule_.expect(IndexOutOfBoundsException.class);
            exceptionRule_.expectMessage("no space is left. required: 1, space: 0");

            sut_.drainFrom(new ArrayCodecBuffer(new byte[1], 0, 1), 1);
        }

        @Test
        public void testDrainFromWithNegative() throws Exception {
            exceptionRule_.expect(IllegalArgumentException.class);
            sut_.drainFrom(Buffers.newCodecBuffer(1), -1);
        }
    }

    public static class BufferSinkTests extends CodecBufferTestAbstract.AbstractBufferSinkTests {
        @Override
        protected CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length) {
            CodecBuffer b = new ArrayCodecBuffer(buffer, offset, length);
            return new SlicedCodecBuffer(b);
        }
    }

    public static class StructureChangeTests {

        private SlicedCodecBuffer sut_;

        @Rule
        public ExpectedException exceptionRule_ = ExpectedException.none();

        @Before
        public void setUp() {
            byte[] data = new byte[10];
            Arrays.fill(data, (byte) 0);
            CodecBuffer base = new ArrayCodecBuffer(data, 0, data.length);
            base.beginning(3);
            sut_ = new SlicedCodecBuffer(base);
        }

        @Test
        public void testAddFirst_OkIfEnoughSpaceExists() throws Exception {
            byte[] data = new byte[4];
            Arrays.fill(data, (byte) -1);
            CodecBuffer added = new ArrayCodecBuffer(data, 0, data.length);

            sut_.beginning(4);
            sut_.addFirst(added);

            assertThat(sut_.remainingBytes(), is(7));
            byte[] expected = new byte[10];
            expected[3] = (byte) -1;
            expected[4] = (byte) -1;
            expected[5] = (byte) -1;
            expected[6] = (byte) -1;
            assertThat(sut_.array(), is(expected));
        }

        @Test
        public void testAddFirst_NgIfNoEnoughSpaceExists() throws Exception {
            byte[] data = new byte[4];
            CodecBuffer added = new ArrayCodecBuffer(data, 0, data.length);
            exceptionRule_.expect(IndexOutOfBoundsException.class);
            exceptionRule_.expectMessage("no space is left. required: 4, front space: 3");

            sut_.beginning(3);
            sut_.addFirst(added);
        }

        @Test
        public void testAddLast_OkIfEnoughSpaceExists() throws Exception {
            byte[] data = new byte[4];
            Arrays.fill(data, (byte) -1);
            CodecBuffer added = new ArrayCodecBuffer(data, 0, data.length);

            sut_.end(3);
            sut_.addLast(added);

            assertThat(sut_.remainingBytes(), is(7));
            byte[] expected = new byte[10];
            expected[6] = (byte) -1;
            expected[7] = (byte) -1;
            expected[8] = (byte) -1;
            expected[9] = (byte) -1;
            assertThat(sut_.array(), is(expected));
        }

        @Test
        public void testAddLast_NgIfNoEnoughSpaceExists() throws Exception {
            byte[] data = new byte[4];
            CodecBuffer added = new ArrayCodecBuffer(data, 0, data.length);
            exceptionRule_.expect(IndexOutOfBoundsException.class);
            exceptionRule_.expectMessage("no space is left. required: 4, back space: 3");

            sut_.end(4);
            sut_.addLast(added);
        }
    }
}
