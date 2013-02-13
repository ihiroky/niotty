package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
@RunWith(Enclosed.class)
public class ByteBufferDecodeBufferTest {

    public static class EmptyCase {

        private ByteBufferDecodeBuffer sut;

        @Before
        public void setUp() {
            sut = ByteBufferDecodeBuffer.wrap(ByteBuffer.allocate(0));
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadByte() throws Exception {
            sut.readByte();
        }

        @Test
        public void testReadBytes() throws Exception {
            // nothing changes
            sut.readBytes(new byte[0], 0, 0);
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadBytes1() throws Exception {
            sut.readBytes(new byte[1], 0, 1);
        }

        @Test
        public void testReadBytesByteBuffer() throws Exception {
            // nothing changes
            sut.readBytes(ByteBuffer.allocate(0));
        }

        @Test
        public void testReadBytesByteBuffer1() throws Exception {
            // nothing changes
            ByteBuffer b = ByteBuffer.wrap(new byte[]{1});
            sut.readBytes(b);
            assertThat(sut.capacityBytes(), is(0));
            assertThat(b.get(), is((byte)1));
        }

        @Test
        public void testReadBytes4() throws Exception {
            // nothing changes
            sut.readBytes4(0);
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadBytes41() throws Exception {
            sut.readBytes4(1);
        }

        @Test
        public void testReadBytes8() throws Exception {
            // nothing changes
            sut.readBytes8(0);
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadChar() throws Exception {
            sut.readChar();
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadShort() throws Exception {
            sut.readShort();
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadInt() throws Exception {
            sut.readInt();
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadLong() throws Exception {
            sut.readLong();
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadFloat() throws Exception {
            sut.readFloat();
        }

        @Test(expected = BufferUnderflowException.class)
        public void testReadDouble() throws Exception {
            sut.readDouble();
        }

        @Test
        public void testSkipBytes() throws Exception {
            int skipped = sut.skipBytes(0);
            assertThat(skipped, is(0));

            skipped = sut.skipBytes(1);
            assertThat(skipped, is(0));

            skipped = sut.skipBytes(-1);
            assertThat(skipped, is(0));
        }

        @Test
        public void testDrainFrom() throws Exception {
            ArrayDecodeBuffer b = ArrayDecodeBuffer.wrap(new byte[]{1, 2, 3}, 3);
            sut.drainFrom(b);
            assertThat(sut.remainingBytes(), is(3));
            assertThat(sut.capacityBytes(), is(3));
        }
    }

    public static class NormalCase {

        private ByteBufferDecodeBuffer sut;
        private int dataLength;

        @Rule
        public ExpectedException expectedException = ExpectedException.none();

        @Before
        public void setUp() {
            byte[] data = new byte[] {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
            };
            dataLength = data.length;
            sut = ByteBufferDecodeBuffer.wrap(ByteBuffer.wrap(data));
        }

        @Test
        public void testReadByte() throws Exception {
            int b = sut.readByte();
            assertThat(b, is(0x30));

            b = sut.readByte();
            assertThat(b, is(0x31));
        }

        @Test
        public void testReadByteUnderflow() throws Exception {
            for (int i = 0; i < dataLength; i++) {
                sut.readByte();
            }
            expectedException.expect(BufferUnderflowException.class);
            sut.readByte();
        }

        @Test
        public void testReadBytes() throws Exception {
            byte[] b = new byte[4];
            sut.readBytes(b, 0, 4);
            assertThat(sut.remainingBytes(), is(12));
            assertThat(b[0], is((byte) '0'));
            assertThat(b[1], is((byte) '1'));
            assertThat(b[2], is((byte) '2'));
            assertThat(b[3], is((byte) '3'));

            sut.readBytes(b, 0, 4);
            assertThat(sut.remainingBytes(), is(8));
            assertThat(b[0], is((byte) '4'));
            assertThat(b[1], is((byte) '5'));
            assertThat(b[2], is((byte) '6'));
            assertThat(b[3], is((byte) '7'));
        }

        @Test
        public void testReadBytesFront() throws Exception {
            byte[] b = new byte[4];
            sut.readBytes(b, 2, 2);
            assertThat(sut.remainingBytes(), is(14));
            assertThat(b[2], is((byte) '0'));
            assertThat(b[3], is((byte) '1'));
        }

        @Test
        public void testReadBytesTail() throws Exception {
            byte[] b = new byte[4];
            sut.readBytes(b, 0, 2);
            assertThat(sut.remainingBytes(), is(14));
            assertThat(b[0], is((byte) '0'));
            assertThat(b[1], is((byte) '1'));
        }

        @Test
        public void testReadBytesUnderflow() throws Exception {
            byte[] ok = new byte[dataLength];
            sut.readBytes(ok, 0, ok.length);

            expectedException.expect(BufferUnderflowException.class);
            byte[] ng = new byte[dataLength + 1];
            sut.readBytes(ng, 0, ng.length);
        }

        @Test
        public void testReadBytesByteBuffer() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(4);
            sut.readBytes(bb);
            bb.flip();
            assertThat(sut.remainingBytes(), is(12));
            assertThat(bb.get(), is((byte) '0'));
            assertThat(bb.get(), is((byte) '1'));
            assertThat(bb.get(), is((byte) '2'));
            assertThat(bb.get(), is((byte) '3'));

            bb.clear();
            sut.readBytes(bb);
            bb.flip();
            assertThat(sut.remainingBytes(),is(8));
            assertThat(bb.get(), is((byte) '4'));
            assertThat(bb.get(), is((byte) '5'));
            assertThat(bb.get(), is((byte) '6'));
            assertThat(bb.get(), is((byte) '7'));
        }

        @Test
        public void testReadByteBufferHead() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.limit(2);
            sut.readBytes(bb);
            bb.flip();
            assertThat(sut.remainingBytes(), is(14));
            assertThat(bb.get(), is((byte) '0'));
            assertThat(bb.get(), is((byte) '1'));
            assertThat(bb.hasRemaining(), is(false));
        }

        @Test
        public void testReadByteBufferTail() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.position(2);
            sut.readBytes(bb);
            bb.position(2);
            assertThat(sut.remainingBytes(), is(14));
            assertThat(bb.get(), is((byte) '0'));
            assertThat(bb.get(), is((byte) '1'));
            assertThat(bb.hasRemaining(), is(false));
        }

        @Test
        public void testReadBytesByteBufferWhole() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(20);
            sut.readBytes(bb);
            bb.flip();
            assertThat(sut.remainingBytes(), is(0));
            assertThat(bb.remaining(), is(16));
            assertThat(bb.get(0), is((byte) '0'));
            assertThat(bb.get(15), is((byte)'f'));
        }

        @Test
        public void testReadBytes8() throws Exception {
            long value = sut.readBytes8(0);
            assertThat(value, is(0L));
            assertThat(sut.remainingBytes(), is(16));

            value = sut.readBytes8(1);
            assertThat(value, is(0x30L));
            assertThat(sut.remainingBytes(), is(15));

            value = sut.readBytes8(8);
            assertThat(value, is(0x3132333435363738L));
            assertThat(sut.remainingBytes(), is(7));
        }

        @Test
        public void testReadBytes8UnderRangeCheck() throws Exception {
            expectedException.expect(IllegalArgumentException.class);
            sut.readBytes8(-1);
        }

        @Test
        public void testReadBytes8OverRangeCheck() throws Exception {
            expectedException.expect(IllegalArgumentException.class);
            sut.readBytes8(9);
        }

        @Test
        public void testReadBytes8Underflow() throws Exception {
            sut.readBytes8(1);
            sut.readBytes8(8);
            assertThat(sut.remainingBytes(), is(7));

            expectedException.expect(BufferUnderflowException.class);
            sut.readBytes8(8);
        }

        @Test
        public void testReadBytes4() throws Exception {
            int read = sut.readBytes4(2);
            assertThat(read, is(0x3031));
            assertThat(sut.remainingBytes(), is(14));
        }

        @Test
        public void testReadBytes4UnderRangeCheck() throws Exception {
            expectedException.expect(IllegalArgumentException.class);
            sut.readBytes4(-1);
        }

        @Test
        public void testReadBytes4OverRangeCheck() throws Exception {
            expectedException.expect(IllegalArgumentException.class);
            sut.readBytes4(5);
        }

        @Test
        public void testReadChar() throws Exception {
            char c = sut.readChar();
            assertThat(c, is((char) 0x3031));

            c = sut.readChar();
            assertThat(c, is((char) 0x3233));
        }

        @Test
        public void testReadShort() throws Exception {
            short s = sut.readShort();
            assertThat(s, is((short) 0x3031));

            s = sut.readShort();
            assertThat(s,is((short) 0x3233));
        }

        @Test
        public void testReadInt() throws Exception {
            int i = sut.readInt();
            assertThat(i, is(0x30313233));

            i = sut.readInt();
            assertThat(i, is(0x34353637));
        }

        @Test
        public void testReadLong() throws Exception {
            long l = sut.readLong();
            assertThat(l, is(0x3031323334353637L));

            l = sut.readLong();
            assertThat(l, is(0x3839616263646566L));
        }

        @Test
        public void testReadFloat() throws Exception {
            float f = sut.readFloat();
            assertThat(Float.floatToIntBits(f), is(0x30313233));
        }

        @Test
        public void testReadDouble() throws Exception {
            double d = sut.readDouble();
            assertThat(Double.doubleToLongBits(d), is(0x3031323334353637L));
        }

        @Test
        public void testSkipBytesForward() throws Exception {
            int skipped = sut.skipBytes(7);
            assertThat(skipped, is(7));
            assertThat(sut.remainingBytes(), is(9));
        }

        @Test
        public void testSkipBytesForwardOver() throws Exception {
            int skipped = sut.skipBytes(17);
            assertThat(skipped, is(16));
            assertThat(sut.remainingBytes(), is(0));
        }

        @Test
        public void testSkipBytesBackward() throws Exception {
            sut.skipBytes(16);
            int skipped = sut.skipBytes(-7);
            assertThat(skipped, is(-7));
            assertThat(sut.remainingBytes(), is(7));
        }

        @Test
        public void testSkipBytesBackwardUnder() throws Exception {
            int skipped = sut.skipBytes(-1);
            assertThat(skipped, is(0));
            assertThat(sut.remainingBytes(), is(16));
        }

        @Test
        public void testDrainFrom() throws Exception {
            // drain 10 bytes
            byte[] a = new byte[10];
            Arrays.fill(a, (byte) 'a');
            sut.reset();
            sut.drainFrom(ArrayDecodeBuffer.wrap(a, a.length));

            assertThat(sut.remainingBytes(), is(10));
            assertThat(sut.capacityBytes(), is(16));

            // drain 10 bytes
            byte[] b = new byte[10];
            Arrays.fill(b, (byte) 'b');
            sut.drainFrom(ArrayDecodeBuffer.wrap(b, b.length));
            assertThat(sut.remainingBytes(), is(20));
            assertThat(sut.capacityBytes(), is(32));

            // drain 50 bytes
            byte[] c = new byte[50];
            Arrays.fill(c, (byte) 'c');
            sut.drainFrom(ArrayDecodeBuffer.wrap(c, c.length));
            assertThat(sut.remainingBytes(), is(70));
            assertThat(sut.capacityBytes(), is(70));

            byte[] ea = new byte[a.length];
            sut.readBytes(ea, 0, ea.length);
            assertThat(ea, is(a));

            byte[] eb = new byte[b.length];
            sut.readBytes(eb, 0, eb.length);
            assertThat(eb, is(b));

            byte[] ec = new byte[c.length];
            sut.readBytes(ec, 0, ec.length);
            assertThat(ec, is(c));

            assertThat(sut.remainingBytes(), is(0));
        }
    }
}
