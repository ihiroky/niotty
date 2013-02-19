package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

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
public class ArrayDecodeBufferTest {

    public static class EmptyCase {

        private ArrayDecodeBuffer sut;

        @Rule
        public ExpectedException expectedException = ExpectedException.none();

        @Before
        public void setUp() {
            sut = new ArrayDecodeBuffer();
            sut.drainFrom(Buffers.newDecodeBuffer(new byte[0], 0, 0));
        }

        @Test
        public void testReadByte() throws Exception {
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readByte();
        }

        @Test
        public void testReadBytes() throws Exception {
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readBytes(new byte[1], 0, 1);
        }

        @Test
        public void testReadBytesByteBuffer() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(1);
            sut.readBytes(bb);
            assertThat(bb.remaining(), is(1));
            assertThat(sut.remainingBytes(), is(0));
        }

        @Test
        public void testReadBytes4() throws Exception {
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readBytes4(1);
        }

        @Test
        public void testReadBytes8() throws Exception {
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readBytes8(1);
        }

        @Test
        public void testReadChar() throws Exception {
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readChar();
        }

        @Test
        public void testReadShort() throws Exception {
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readShort();
        }

        @Test
        public void testReadInt() throws Exception {
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readInt();
        }

        @Test
        public void testReadLong() throws Exception {
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readLong();
        }

        @Test
        public void testReadFloat() throws Exception {
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readFloat();
        }

        @Test
        public void testReadDouble() throws Exception {
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readDouble();
        }

        @Test
        public void testSkipBytes0() throws Exception {
            int skipped = sut.skipBytes(0);
            assertThat(skipped, is(0));
            assertThat(sut.remainingBytes(), is(0));
        }

        @Test
        public void testSkipBytesForward() throws Exception {
            int skipped = sut.skipBytes(1);
            assertThat(skipped, is(0));
            assertThat(sut.remainingBytes(), is(0));
        }

        @Test
        public void testSkipBytesBackward() throws Exception {
            int skipped = sut.skipBytes(-1);
            assertThat(skipped, is(0));
            assertThat(sut.remainingBytes(), is(0));
        }

        @Test
        public void testRemainingBytes() throws Exception {
            assertThat(sut.remainingBytes(), is(0));
        }

        @Test
        public void testCapacityBytes() throws Exception {
            assertThat(sut.limitBytes(), is(0));
        }
    }

    public static class NormalCase {

        private ArrayDecodeBuffer sut;

        @Rule
        public ExpectedException expectedException = ExpectedException.none();

        @Before
        public void before() {
            byte[] buffer = new byte[50];
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 10; j ++) {
                    buffer[i * 10 + j] = (byte) (j + '0');
                }
            }
            sut = new ArrayDecodeBuffer(buffer, 0, buffer.length);
        }

        @Test
        public void testReadByte() throws Exception {
            for (int i = 0; i < 10; i++) {
                assertThat(sut.readByte(), is(i + 0x30));
            }
        }

        @Test
        public void testReadByteOverflow() throws Exception {
            for (int i = 0; i < 50; i++) {
                sut.readByte();
            }
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readByte();
        }

        @Test
        public void testReadBytes4() throws Exception {
            int byte0 = sut.readBytes4(0);
            assertThat(byte0, is(0));

            int byte1 = sut.readBytes4(1);
            assertThat(byte1, is(0x30));

            int byte2 = sut.readBytes4(2);
            assertThat(byte2, is(0x3132));

            int byte3 = sut.readBytes4(3);
            assertThat(byte3, is(0x333435));

            int byte4 = sut.readBytes4(4);
            assertThat(byte4, is(0x36373839));
        }

        @Test
        public void testReadBytes4Over4Bytes() throws Exception {
            expectedException.expect(IllegalArgumentException.class);
            expectedException.expectMessage("bytes must be in [0, 4].");
            sut.readBytes4(5);
        }

        @Test
        public void testReadBytes4Under4Bytes() throws Exception {
            expectedException.expect(IllegalArgumentException.class);
            expectedException.expectMessage("bytes must be in [0, 4].");
            sut.readBytes4(-1);
        }

        @Test
        public void testReadBytes4Underflow() throws Exception {
            while (sut.remainingBytes() >= 4) {
                sut.readBytes4(4);
            }
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readBytes4(4);
        }

        @Test
        public void testReadBytes8() throws Exception {
            long b0 = sut.readBytes8(0);
            assertThat(b0, is(0L));

            long b1 = sut.readBytes8(1);
            assertThat(b1, is(0x30L));

            long b2 = sut.readBytes8(2);
            assertThat(b2, is(0x3132L));

            long b7 = sut.readBytes8(7);
            assertThat(b7, is(0x33343536373839L));

            long b8 = sut.readBytes8(8);
            assertThat(b8, is(0x3031323334353637L));
        }

        @Test
        public void testReadBytes8Over8Bytes() throws Exception {
            expectedException.expect(IllegalArgumentException.class);
            expectedException.expectMessage("bytes must be in [0, 8].");
            sut.readBytes8(9);
        }

        @Test
        public void testReadBytes8Under8Bytes() throws Exception {
            expectedException.expect(IllegalArgumentException.class);
            expectedException.expectMessage("bytes must be in [0, 8].");
            sut.readBytes8(-1);
        }

        @Test
        public void testReadBytes8Underflow() throws Exception {
            while (sut.remainingBytes() >= 8) {
                sut.readBytes8(8);
            }
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readBytes8(8);
        }

        @Test
        public void testReadBytes() throws Exception {
            byte[] data = new byte[4];
            sut.readBytes(data, 0, 0);
            assertThat(data, is(new byte[]{0, 0, 0, 0}));

            sut.readBytes(data, 3, 1);
            assertThat(data, is(new byte[]{0, 0, 0, 0x30}));

            sut.readBytes(data, 0, 4);
            assertThat(data, is(new byte[]{0x31, 0x32, 0x33, 0x34}));
        }

        @Test
        public void testReadBytesUnderflow() throws Exception {
            int remaining = sut.remainingBytes();
            byte[] data = new byte[remaining + 10];
            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readBytes(data, 0, data.length);
        }

        @Test
        public void testReadBytesByteBufferPart() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.limit(0);
            sut.readBytes(bb);
            assertThat(bb.array(), is(new byte[]{0, 0, 0, 0}));
            assertThat(sut.remainingBytes(), is(50));

            bb.limit(4).position(3);
            sut.readBytes(bb);
            assertThat(bb.array(), is(new byte[]{0, 0, 0, 0x30}));
            assertThat(sut.remainingBytes(), is(49));

            bb.clear();
            sut.readBytes(bb);
            assertThat(bb.array(), is(new byte[]{0x31, 0x32, 0x33, 0x34}));
            assertThat(sut.remainingBytes(), is(45));
        }

        @Test
        public void testReadBytesByteBufferWhole() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(60);
            sut.readBytes(bb);
            bb.flip();

            assertThat(sut.remainingBytes(), is(0));
            assertThat(bb.remaining(), is(50));
            for (int i = 0x30; i < 0x3a; i++) {
                assertThat(bb.get(), is((byte) i));
            }
            bb.position(40);
            for (int i = 0x30; i < 0x3a; i++) {
                assertThat(bb.get(), is((byte) i));
            }
        }

        @Test
        public void testReadChar() throws Exception {
            char c = sut.readChar();
            assertThat(c, is((char) 0x3031));
        }

        @Test
        public void testReadCharUnderflow() throws Exception {
            int remaining = sut.remainingBytes();
            sut.readBytes(new byte[remaining - 1], 0, remaining - 1);

            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readChar();
        }

        @Test
        public void testReadShort() throws Exception {
            short s = sut.readShort();
            assertThat(s, is((short) 0x3031));
        }

        @Test
        public void testReadShortUnderflow() throws Exception {
            int remaining = sut.remainingBytes();
            sut.readBytes(new byte[remaining - 1], 0, remaining - 1);

            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readShort();
        }

        @Test
        public void testReadInt() throws Exception {
            int i = sut.readInt();
            assertThat(i, is(0x30313233));
        }

        @Test
        public void testReadIntUnderflow() throws Exception {
            int remaining = sut.remainingBytes();
            sut.readBytes(new byte[remaining - 1], 0, remaining - 1);

            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readInt();
        }

        @Test
        public void testReadLong() throws Exception {
            long i = sut.readLong();
            assertThat(i, is(0x3031323334353637L));
        }

        @Test
        public void testReadLongUnderflow() throws Exception {
            int remaining = sut.remainingBytes();
            sut.readBytes(new byte[remaining - 1], 0, remaining - 1);

            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readLong();
        }

        @Test
        public void testReadFloat() throws Exception {
            float v = sut.readFloat();
            assertThat(v, is(Float.intBitsToFloat(0x30313233)));
        }

        @Test
        public void testReadFloatUnderflow() throws Exception {
            int remaining = sut.remainingBytes();
            sut.readBytes(new byte[remaining - 1], 0, remaining - 1);

            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readFloat();
        }

        @Test
        public void testReadDouble() throws Exception {
            double v = sut.readDouble();
            assertThat(v, is(Double.longBitsToDouble(0x3031323334353637L)));
        }

        @Test
        public void testReadDoubleUnderflow() throws Exception {
            int remaining = sut.remainingBytes();
            sut.readBytes(new byte[remaining - 1], 0, remaining - 1);

            expectedException.expect(IndexOutOfBoundsException.class);
            sut.readDouble();
        }

        @Test
        public void testReadString() throws Exception {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
            ArrayEncodeBuffer buffer = new ArrayEncodeBuffer();
            buffer.writeString(encoder, "0123");
            sut = new ArrayDecodeBuffer(buffer.toArray(), 0, buffer.filledBytes());

            String s = sut.readString(decoder);
            assertThat(s, is("0123"));
        }

        @Test
        public void testSkipBytesForward() throws Exception {
            int skipped = sut.skipBytes(7);
            assertThat(skipped, is(7));
            assertThat(sut.remainingBytes(), is(43));
        }

        @Test
        public void testSkipBytesForwardOver() throws Exception {
            int skipped = sut.skipBytes(51);
            assertThat(skipped, is(50));
            assertThat(sut.remainingBytes(), is(0));
        }

        @Test
        public void testSkipBytesBackward() throws Exception {
            sut.skipBytes(50);
            int skipped = sut.skipBytes(-7);
            assertThat(skipped, is(-7));
            assertThat(sut.remainingBytes(), is(7));
        }

        @Test
        public void testSkipBytesBackwardUnder() throws Exception {
            int skipped = sut.skipBytes(-10);
            assertThat(skipped, is(0));
            assertThat(sut.remainingBytes(), is(50));
        }

        @Test
        public void testRemainingBytes() throws Exception {
            int r = sut.remainingBytes();
            assertThat(r, is(50));

            sut.readLong();
            r = sut.remainingBytes();
            assertThat(r, is(42));
        }

        @Test
        public void testCapacityBytes() throws Exception {
            assertThat(sut.limitBytes(), is(50));
        }

        @Test
        public void testClear() throws Exception {
            sut.clear();
            assertThat(sut.remainingBytes(), is(0));
        }

        @Test
        public void testDrainFrom() throws Exception {
            // drain 40 bytes
            byte[] a = new byte[40];
            Arrays.fill(a, (byte) 'a');
            sut.clear();
            sut.drainFrom(new ArrayDecodeBuffer(a, 0, a.length));

            assertThat(sut.remainingBytes(), is(40));
            assertThat(sut.limitBytes(), is(40));

            // drain 20 bytes
            byte[] b = new byte[20];
            Arrays.fill(b, (byte) 'b');
            sut.drainFrom(new ArrayDecodeBuffer(b, 0, b.length));
            assertThat(sut.remainingBytes(), is(60));
            assertThat(sut.limitBytes(), is(60));

            // drain 150 bytes
            byte[] c = new byte[150];
            Arrays.fill(c, (byte) 'c');
            sut.drainFrom(new ArrayDecodeBuffer(c, 0, c.length));
            assertThat(sut.remainingBytes(), is(210));
            assertThat(sut.limitBytes(), is(210));

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

        @Test
        public void testHasArray() throws Exception {
            assertThat(sut.hasArray(), is(true));
        }

        @Test
        public void testToArray() throws Exception {
            byte[] array = sut.toArray();
            array[0] = 'a';
            assertThat(sut.readByte(), is((int) 'a')); // read 1 byte
            assertThat(array.length, is(sut.remainingBytes() + 1));
        }

        @Test
        public void testArrayOffset() throws Exception {
            assertThat(sut.arrayOffset(), is(0));
            assertThat(new ArrayEncodeBuffer(new byte[1], 1, 0).arrayOffset(), is(1));
        }
    }
}
