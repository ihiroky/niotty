package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
@RunWith(Enclosed.class)
public class ByteBufferDecodeBufferTest {

    public static class EmptyCase {

        private ByteBufferDecodeBuffer sut_;

        @Before
        public void setUp() {
            sut_ = new ByteBufferDecodeBuffer(ByteBuffer.allocate(0));
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadByte() throws Exception {
            sut_.readByte();
        }

        @Test
        public void testReadBytes() throws Exception {
            // nothing changes
            sut_.readBytes(new byte[0], 0, 0);
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadBytes1() throws Exception {
            sut_.readBytes(new byte[1], 0, 1);
        }

        @Test
        public void testReadBytesByteBuffer() throws Exception {
            // nothing changes
            sut_.readBytes(ByteBuffer.allocate(0));
        }

        @Test
        public void testReadBytesByteBuffer1() throws Exception {
            // nothing changes
            ByteBuffer b = ByteBuffer.wrap(new byte[]{1});
            sut_.readBytes(b);
            assertThat(sut_.limitBytes(), is(0));
            assertThat(b.get(), is((byte)1));
        }

        @Test
        public void testReadBytes4() throws Exception {
            // nothing changes
            sut_.readBytes4(0);
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadBytes41() throws Exception {
            sut_.readBytes4(1);
        }

        @Test
        public void testReadBytes8() throws Exception {
            // nothing changes
            sut_.readBytes8(0);
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadChar() throws Exception {
            sut_.readChar();
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadShort() throws Exception {
            sut_.readShort();
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadInt() throws Exception {
            sut_.readInt();
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadLong() throws Exception {
            sut_.readLong();
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadFloat() throws Exception {
            sut_.readFloat();
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadDouble() throws Exception {
            sut_.readDouble();
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadString() throws Exception {
            sut_.readString(StandardCharsets.UTF_8.newDecoder());
        }

        @Test
        public void testSkipBytes() throws Exception {
            int skipped = sut_.skipBytes(0);
            assertThat(skipped, is(0));

            skipped = sut_.skipBytes(1);
            assertThat(skipped, is(0));

            skipped = sut_.skipBytes(-1);
            assertThat(skipped, is(0));
        }

        @Test
        public void testDrainFrom() throws Exception {
            DecodeBuffer b = Buffers.newDecodeBuffer(new byte[]{1, 2, 3}, 0, 3);
            sut_.drainFrom(b);
            assertThat(sut_.remainingBytes(), is(3));
            assertThat(sut_.limitBytes(), is(3));
        }

        @Test
        public void testSlice() throws Exception {
            DecodeBuffer sliced = sut_.slice(0);
            assertThat(sliced.remainingBytes(), is(0));
        }

        @Test(expected = IllegalArgumentException.class)
        public void testSliceNegative() throws Exception {
            sut_.slice(-1);
        }

        @Test(expected = IllegalArgumentException.class)
        public void testSliceOverRemaining() throws Exception {
            sut_.slice(1);
        }

    }

    public static class NormalCase {

        private ByteBufferDecodeBuffer sut_;
        private int dataLength_;

        @Rule
        public ExpectedException exceptionRule_ = ExpectedException.none();

        @Before
        public void setUp() {
            byte[] data = new byte[] {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
            };
            dataLength_ = data.length;
            sut_ = new ByteBufferDecodeBuffer(ByteBuffer.wrap(data));
        }

        @Test
        public void testReadByte() throws Exception {
            int b = sut_.readByte();
            assertThat(b, is(0x30));

            b = sut_.readByte();
            assertThat(b, is(0x31));
        }

        @Test
        public void testReadByteUnderflow() throws Exception {
            for (int i = 0; i < dataLength_; i++) {
                sut_.readByte();
            }
            exceptionRule_.expect(BufferUnderflowException.class);
            sut_.readByte();
        }

        @Test
        public void testReadBytes() throws Exception {
            byte[] b = new byte[4];
            sut_.readBytes(b, 0, 4);
            assertThat(sut_.remainingBytes(), is(12));
            assertThat(b[0], is((byte) '0'));
            assertThat(b[1], is((byte) '1'));
            assertThat(b[2], is((byte) '2'));
            assertThat(b[3], is((byte) '3'));

            sut_.readBytes(b, 0, 4);
            assertThat(sut_.remainingBytes(), is(8));
            assertThat(b[0], is((byte) '4'));
            assertThat(b[1], is((byte) '5'));
            assertThat(b[2], is((byte) '6'));
            assertThat(b[3], is((byte) '7'));
        }

        @Test
        public void testReadBytesFront() throws Exception {
            byte[] b = new byte[4];
            sut_.readBytes(b, 2, 2);
            assertThat(sut_.remainingBytes(), is(14));
            assertThat(b[2], is((byte) '0'));
            assertThat(b[3], is((byte) '1'));
        }

        @Test
        public void testReadBytesTail() throws Exception {
            byte[] b = new byte[4];
            sut_.readBytes(b, 0, 2);
            assertThat(sut_.remainingBytes(), is(14));
            assertThat(b[0], is((byte) '0'));
            assertThat(b[1], is((byte) '1'));
        }

        @Test
        public void testReadBytesUnderflow() throws Exception {
            byte[] ok = new byte[dataLength_];
            sut_.readBytes(ok, 0, ok.length);

            exceptionRule_.expect(BufferUnderflowException.class);
            byte[] ng = new byte[dataLength_ + 1];
            sut_.readBytes(ng, 0, ng.length);
        }

        @Test
        public void testReadBytesByteBuffer() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(4);
            sut_.readBytes(bb);
            bb.flip();
            assertThat(sut_.remainingBytes(), is(12));
            assertThat(bb.get(), is((byte) '0'));
            assertThat(bb.get(), is((byte) '1'));
            assertThat(bb.get(), is((byte) '2'));
            assertThat(bb.get(), is((byte) '3'));

            bb.clear();
            sut_.readBytes(bb);
            bb.flip();
            assertThat(sut_.remainingBytes(), is(8));
            assertThat(bb.get(), is((byte) '4'));
            assertThat(bb.get(), is((byte) '5'));
            assertThat(bb.get(), is((byte) '6'));
            assertThat(bb.get(), is((byte) '7'));
        }

        @Test
        public void testReadByteBufferHead() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.limit(2);
            sut_.readBytes(bb);
            bb.flip();
            assertThat(sut_.remainingBytes(), is(14));
            assertThat(bb.get(), is((byte) '0'));
            assertThat(bb.get(), is((byte) '1'));
            assertThat(bb.hasRemaining(), is(false));
        }

        @Test
        public void testReadByteBufferTail() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.position(2);
            sut_.readBytes(bb);
            bb.position(2);
            assertThat(sut_.remainingBytes(), is(14));
            assertThat(bb.get(), is((byte) '0'));
            assertThat(bb.get(), is((byte) '1'));
            assertThat(bb.hasRemaining(), is(false));
        }

        @Test
        public void testReadBytesByteBufferWhole() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(20);
            sut_.readBytes(bb);
            bb.flip();
            assertThat(sut_.remainingBytes(), is(0));
            assertThat(bb.remaining(), is(16));
            assertThat(bb.get(0), is((byte) '0'));
            assertThat(bb.get(15), is((byte)'f'));
        }

        @Test
        public void testReadBytes8() throws Exception {
            long value = sut_.readBytes8(0);
            assertThat(value, is(0L));
            assertThat(sut_.remainingBytes(), is(16));

            value = sut_.readBytes8(1);
            assertThat(value, is(0x30L));
            assertThat(sut_.remainingBytes(), is(15));

            value = sut_.readBytes8(8);
            assertThat(value, is(0x3132333435363738L));
            assertThat(sut_.remainingBytes(), is(7));
        }

        @Test
        public void testReadBytes8UnderRangeCheck() throws Exception {
            exceptionRule_.expect(IllegalArgumentException.class);
            sut_.readBytes8(-1);
        }

        @Test
        public void testReadBytes8OverRangeCheck() throws Exception {
            exceptionRule_.expect(IllegalArgumentException.class);
            sut_.readBytes8(9);
        }

        @Test
        public void testReadBytes8Underflow() throws Exception {
            sut_.readBytes8(1);
            sut_.readBytes8(8);
            assertThat(sut_.remainingBytes(), is(7));

            exceptionRule_.expect(BufferUnderflowException.class);
            sut_.readBytes8(8);
        }

        @Test
        public void testReadBytes4() throws Exception {
            int read = sut_.readBytes4(2);
            assertThat(read, is(0x3031));
            assertThat(sut_.remainingBytes(), is(14));
        }

        @Test
        public void testReadBytes4UnderRangeCheck() throws Exception {
            exceptionRule_.expect(IllegalArgumentException.class);
            sut_.readBytes4(-1);
        }

        @Test
        public void testReadBytes4OverRangeCheck() throws Exception {
            exceptionRule_.expect(IllegalArgumentException.class);
            sut_.readBytes4(5);
        }

        @Test
        public void testReadChar() throws Exception {
            char c = sut_.readChar();
            assertThat(c, is((char) 0x3031));

            c = sut_.readChar();
            assertThat(c, is((char) 0x3233));
        }

        @Test
        public void testReadShort() throws Exception {
            short s = sut_.readShort();
            assertThat(s, is((short) 0x3031));

            s = sut_.readShort();
            assertThat(s,is((short) 0x3233));
        }

        @Test
        public void testReadInt() throws Exception {
            int i = sut_.readInt();
            assertThat(i, is(0x30313233));

            i = sut_.readInt();
            assertThat(i, is(0x34353637));
        }

        @Test
        public void testReadLong() throws Exception {
            long l = sut_.readLong();
            assertThat(l, is(0x3031323334353637L));

            l = sut_.readLong();
            assertThat(l, is(0x3839616263646566L));
        }

        @Test
        public void testReadFloat() throws Exception {
            float f = sut_.readFloat();
            assertThat(Float.floatToIntBits(f), is(0x30313233));
        }

        @Test
        public void testReadDouble() throws Exception {
            double d = sut_.readDouble();
            assertThat(Double.doubleToLongBits(d), is(0x3031323334353637L));
        }

        @Test
        public void testReadString() throws Exception {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
            ArrayEncodeBuffer buffer = new ArrayEncodeBuffer();
            buffer.writeString(encoder, "0123");
            sut_ = new ByteBufferDecodeBuffer(ByteBuffer.wrap(buffer.toArray(), 0, buffer.filledBytes()));

            String s = sut_.readString(decoder);
            assertThat(s, is("0123"));
        }

        @Test
        public void testSkipBytesForward() throws Exception {
            int skipped = sut_.skipBytes(7);
            assertThat(skipped, is(7));
            assertThat(sut_.remainingBytes(), is(9));
        }

        @Test
        public void testSkipBytesForwardOver() throws Exception {
            int skipped = sut_.skipBytes(17);
            assertThat(skipped, is(16));
            assertThat(sut_.remainingBytes(), is(0));
        }

        @Test
        public void testSkipBytesBackward() throws Exception {
            sut_.skipBytes(16);
            int skipped = sut_.skipBytes(-7);
            assertThat(skipped, is(-7));
            assertThat(sut_.remainingBytes(), is(7));
        }

        @Test
        public void testSkipBytesBackwardUnder() throws Exception {
            int skipped = sut_.skipBytes(-1);
            assertThat(skipped, is(0));
            assertThat(sut_.remainingBytes(), is(16));
        }

        @Test
        public void testDrainFrom() throws Exception {
            // drain 10 bytes
            byte[] a = new byte[10];
            Arrays.fill(a, (byte) 'a');
            sut_.clear();
            int drained = sut_.drainFrom(Buffers.newDecodeBuffer(a, 0, a.length));
            assertThat(drained, is(10));
            assertThat(sut_.remainingBytes(), is(10));
            assertThat(sut_.limitBytes(), is(10));

            // drain 10 bytes
            byte[] b = new byte[10];
            Arrays.fill(b, (byte) 'b');
            drained = sut_.drainFrom(Buffers.newDecodeBuffer(b, 0, b.length));
            assertThat(drained, is(10));
            assertThat(sut_.remainingBytes(), is(20));
            assertThat(sut_.limitBytes(), is(20));

            // drain 50 bytes
            byte[] c = new byte[50];
            Arrays.fill(c, (byte) 'c');
            drained = sut_.drainFrom(Buffers.newDecodeBuffer(c, 0, c.length));
            assertThat(drained, is(50));
            assertThat(sut_.remainingBytes(), is(70));
            assertThat(sut_.limitBytes(), is(70));

            byte[] ea = new byte[a.length];
            sut_.readBytes(ea, 0, ea.length);
            assertThat(ea, is(a));

            byte[] eb = new byte[b.length];
            sut_.readBytes(eb, 0, eb.length);
            assertThat(eb, is(b));

            byte[] ec = new byte[c.length];
            sut_.readBytes(ec, 0, ec.length);
            assertThat(ec, is(c));

            assertThat(sut_.remainingBytes(), is(0));
        }

        @Test
        public void testDrainFromWithLimit() throws Exception {
            byte[] a = new byte[40];
            Arrays.fill(a, (byte) 'a');
            DecodeBuffer input = new ArrayDecodeBuffer(a, 0, a.length);
            sut_.clear();

            // drain 30 bytes.
            int drained = sut_.drainFrom(input, 30);
            assertThat(drained, is(30));
            assertThat(sut_.remainingBytes(), is(30));
            assertThat(sut_.limitBytes(), is(30));

            // try to drain 20 bytes, but drain 10 bytes
            drained = sut_.drainFrom(input, 20);
            assertThat(drained, is(10));
            assertThat(sut_.remainingBytes(), is(40));
            assertThat(sut_.limitBytes(), is(40));
        }

        @Test
        public void testDrainFromWithNegative() throws Exception {
            exceptionRule_.expect(IllegalArgumentException.class);
            sut_.drainFrom(Buffers.newDecodeBuffer(1), -1);
        }

        @Test
        public void testHasArray() throws Exception {
            assertThat(sut_.hasArray(), is(true));
        }

        @Test
        public void testToArray() throws Exception {
            byte[] array = sut_.toArray();
            array[0] = 'a';
            assertThat(sut_.readByte(), is((int) 'a')); // read 1 byte
            assertThat(array.length, is(sut_.remainingBytes() + 1));
        }

        @Test
        public void testArrayOffset() throws Exception {
            assertThat(sut_.arrayOffset(), is(0));
            ByteBuffer b = ByteBuffer.wrap(new byte[1], 0, 1);
            b.position(1);
            assertThat(new ByteBufferDecodeBuffer(b.slice()).arrayOffset(), is(1));
        }
        @Test
        public void testSlice() throws Exception {
            byte[] buffer = new byte[3];

            DecodeBuffer sliced = sut_.slice(3);

            sliced.readBytes(buffer, 0, buffer.length);
            assertThat(buffer, is(new byte[]{'0', '1', '2'}));
            assertThat(sliced.remainingBytes(), is(0));
            assertThat(sut_.remainingBytes(), is(13));
        }

        @Test
        public void testSliceShare() throws Exception {
            byte[] buffer = new byte[5];
            DecodeBuffer input = Buffers.newDecodeBuffer(new byte[]{'a', 'b'}, 0, 2);

            DecodeBuffer sliced = sut_.slice(3);
            sliced.drainFrom(input);

            sliced.readBytes(buffer, 0, 5);
            assertThat(buffer, is(new byte[]{'0', '1', '2', 'a', 'b'}));
            sut_.readBytes(buffer, 0, 2);
            assertThat(Arrays.copyOf(buffer, 2), is(new byte[]{'a', 'b'}));
        }

        @Test
        public void testSliceNegative() throws Exception {
            exceptionRule_.expect(IllegalArgumentException.class);
            sut_.slice(-1);
        }

        @Test
        public void testSliceOverRemaining() throws Exception {
            exceptionRule_.expect(IllegalArgumentException.class);
            sut_.slice(17);
        }
    }
}
