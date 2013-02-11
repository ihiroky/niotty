package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
@RunWith(Enclosed.class)
public class ByteBufferDecodeBufferTest {

    public static class EmptyCase {
        @Before
        public void setUp() {
        }

        @Test
        public void test() throws Exception {

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
    }
}
