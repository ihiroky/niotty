package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * TODO write empty case
 * @author Hiroki Itoh
 */
public class CodecBufferTestAbstract {

    public static abstract class AbstractEmptyTests {

        private CodecBuffer sut_;

        @Rule
        public ExpectedException exceptionRule_ = ExpectedException.none();

        protected abstract CodecBuffer createCodecBuffer();

        @Before
        public void setUp() {
            sut_ = createCodecBuffer();
        }

        @Test
        public void testReadByte() throws Exception {
            exceptionRule_.expect(RuntimeException.class);
            sut_.readByte();
        }

        @Test
        public void testReadBytes() throws Exception {
            byte[] b = new byte[]{-1};
            int read = sut_.readBytes(b, 0, b.length);
            assertThat(read, is(0));
            assertThat(b[0], is((byte) -1));
        }

        @Test
        public void testReadBytesByteBuffer() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(1);
            sut_.readBytes(bb);
            assertThat(bb.remaining(), is(1));
            assertThat(sut_.remainingBytes(), is(0));
        }

        @Test
        public void testReadChar() throws Exception {
            exceptionRule_.expect(RuntimeException.class);
            sut_.readChar();
        }

        @Test
        public void testReadShort() throws Exception {
            exceptionRule_.expect(RuntimeException.class);
            sut_.readShort();
        }

        @Test
        public void testReadInt() throws Exception {
            exceptionRule_.expect(RuntimeException.class);
            sut_.readInt();
        }

        @Test
        public void testReadLong() throws Exception {
            exceptionRule_.expect(RuntimeException.class);
            sut_.readLong();
        }

        @Test
        public void testReadFloat() throws Exception {
            exceptionRule_.expect(RuntimeException.class);
            sut_.readFloat();
        }

        @Test
        public void testReadDouble() throws Exception {
            exceptionRule_.expect(RuntimeException.class);
            sut_.readDouble();
        }

        @Test
        public void testSkipBytes0() throws Exception {
            int skipped = sut_.skipBytes(0);
            assertThat(skipped, is(0));
            assertThat(sut_.remainingBytes(), is(0));
        }

        @Test
        public void testSkipBytesForward() throws Exception {
            int skipped = sut_.skipBytes(1);
            assertThat(skipped, is(0));
            assertThat(sut_.remainingBytes(), is(0));
        }

        @Test
        public void testSkipBytesBackward() throws Exception {
            int skipped = sut_.skipBytes(-1);
            assertThat(skipped, is(0));
            assertThat(sut_.remainingBytes(), is(0));
        }

        @Test
        public void testRemainingBytes() throws Exception {
            assertThat(sut_.remainingBytes(), is(0));
        }

        @Test
        public void testSpaceBytes() throws Exception {
            assertThat(sut_.spaceBytes(), is(512));
        }
        @Test
        public void testCapacityBytes() throws Exception {
            assertThat(sut_.capacityBytes(), is(512));
        }

        @Test
        public void testSlice() throws Exception {
            CodecBuffer sliced = sut_.slice(0);
            assertThat(sliced.remainingBytes(), is(0));
        }

        @Test
        public void testSliceNegative() throws Exception {
            exceptionRule_.expect(IllegalArgumentException.class);
            sut_.slice(-1);
        }

        @Test
        public void testSliceOverRemaining() throws Exception {
            exceptionRule_.expect(IllegalArgumentException.class);
            sut_.slice(1);
        }

        @Test
        public void testIndexOf_SingleByte() throws Exception {
            assertThat(sut_.indexOf(0, 0), is(-1));
            assertThat(sut_.indexOf(0, 1), is(-1));
            assertThat(sut_.indexOf(0, -1), is(-1));
        }

        @Test
        public void testIndexOf_MultiByte() throws Exception {
            assertThat(sut_.indexOf(new byte[0], 0), is(-1));
            assertThat(sut_.indexOf(new byte[0], 1), is(-1));
            assertThat(sut_.indexOf(new byte[0], -1), is(-1));
        }
    }

    public static abstract class AbstractReadTests {

        private CodecBuffer sut_;

        @Rule
        public ExpectedException exceptionRule_ = ExpectedException.none();

        protected abstract CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length);

        @Before
        public void before() {
            byte[] buffer = new byte[50];
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 10; j ++) {
                    buffer[i * 10 + j] = (byte) (j + '0');
                }
            }
            sut_ = createCodecBuffer(buffer, 0, buffer.length);
        }

        @Test
        public void testReadByte() throws Exception {
            for (int i = 0; i < 10; i++) {
                assertThat(sut_.readByte(), is(i + 0x30));
            }
        }

        @Test
        public void testReadByteOverflow() throws Exception {
            for (int i = 0; i < 50; i++) {
                sut_.readByte();
            }
            exceptionRule_.expect(RuntimeException.class);
            sut_.readByte();
        }

        @Test
        public void testReadBytes() throws Exception {
            byte[] data = new byte[4];
            sut_.readBytes(data, 0, 0);
            assertThat(data, is(new byte[]{0, 0, 0, 0}));

            sut_.readBytes(data, 3, 1);
            assertThat(data, is(new byte[]{0, 0, 0, 0x30}));

            sut_.readBytes(data, 0, 4);
            assertThat(data, is(new byte[]{0x31, 0x32, 0x33, 0x34}));
        }

        @Test
        public void testReadBytesUnderflow() throws Exception {
            int remaining = sut_.remainingBytes();
            byte[] data = new byte[remaining + 10];

            int read = sut_.readBytes(data, 0, data.length);

            assertThat(read, is(remaining));
            assertThat(data[0], is((byte) '0'));
            assertThat(data[49], is((byte) '9'));
            assertThat(data[50], is((byte) 0));
        }

        @Test
        public void testReadBytesByteBufferPart() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.limit(0);
            sut_.readBytes(bb);
            assertThat(bb.array(), is(new byte[]{0, 0, 0, 0}));
            assertThat(sut_.remainingBytes(), is(50));

            bb.limit(4).position(3);
            sut_.readBytes(bb);
            assertThat(bb.array(), is(new byte[]{0, 0, 0, 0x30}));
            assertThat(sut_.remainingBytes(), is(49));

            bb.clear();
            sut_.readBytes(bb);
            assertThat(bb.array(), is(new byte[]{0x31, 0x32, 0x33, 0x34}));
            assertThat(sut_.remainingBytes(), is(45));
        }

        @Test
        public void testReadBytesByteBufferWhole() throws Exception {
            ByteBuffer bb = ByteBuffer.allocate(60);
            sut_.readBytes(bb);
            bb.flip();

            assertThat(sut_.remainingBytes(), is(0));
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
            char c = sut_.readChar();
            assertThat(c, is((char) 0x3031));
        }

        @Test
        public void testReadCharUnderflow() throws Exception {
            int remaining = sut_.remainingBytes();
            sut_.readBytes(new byte[remaining - 1], 0, remaining - 1);

            exceptionRule_.expect(RuntimeException.class);
            sut_.readChar();
        }

        @Test
        public void testReadShort() throws Exception {
            short s = sut_.readShort();
            assertThat(s, is((short) 0x3031));
        }

        @Test
        public void testReadShortUnderflow() throws Exception {
            int remaining = sut_.remainingBytes();
            sut_.readBytes(new byte[remaining - 1], 0, remaining - 1);

            exceptionRule_.expect(RuntimeException.class);
            sut_.readShort();
        }

        @Test
        public void testReadInt() throws Exception {
            int i = sut_.readInt();
            assertThat(i, is(0x30313233));
        }

        @Test
        public void testReadIntUnderflow() throws Exception {
            int remaining = sut_.remainingBytes();
            sut_.readBytes(new byte[remaining - 1], 0, remaining - 1);

            exceptionRule_.expect(RuntimeException.class);
            sut_.readInt();
        }

        @Test
        public void testReadLong() throws Exception {
            long i = sut_.readLong();
            assertThat(i, is(0x3031323334353637L));
        }

        @Test
        public void testReadLongUnderflow() throws Exception {
            int remaining = sut_.remainingBytes();
            sut_.readBytes(new byte[remaining - 1], 0, remaining - 1);

            exceptionRule_.expect(RuntimeException.class);
            sut_.readLong();
        }

        @Test
        public void testReadFloat() throws Exception {
            float v = sut_.readFloat();
            assertThat(v, is(Float.intBitsToFloat(0x30313233)));
        }

        @Test
        public void testReadFloatUnderflow() throws Exception {
            int remaining = sut_.remainingBytes();
            sut_.readBytes(new byte[remaining - 1], 0, remaining - 1);

            exceptionRule_.expect(RuntimeException.class);
            sut_.readFloat();
        }

        @Test
        public void testReadDouble() throws Exception {
            double v = sut_.readDouble();
            assertThat(v, is(Double.longBitsToDouble(0x3031323334353637L)));
        }

        @Test
        public void testReadDoubleUnderflow() throws Exception {
            int remaining = sut_.remainingBytes();
            sut_.readBytes(new byte[remaining - 1], 0, remaining - 1);

            exceptionRule_.expect(RuntimeException.class);
            sut_.readDouble();
        }

        @Test
        public void testReadString() throws Exception {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
            ArrayCodecBuffer buffer = new ArrayCodecBuffer();
            buffer.writeString(encoder, "0123");
            sut_ = new ArrayCodecBuffer(buffer.toArray(), 0, buffer.remainingBytes());

            String s = sut_.readString(decoder);
            assertThat(s, is("0123"));
        }

        @Test
        public void testSkipBytesForward() throws Exception {
            int skipped = sut_.skipBytes(7);
            assertThat(skipped, is(7));
            assertThat(sut_.remainingBytes(), is(43));
        }

        @Test
        public void testSkipBytesForwardOver() throws Exception {
            int skipped = sut_.skipBytes(51);
            assertThat(skipped, is(50));
            assertThat(sut_.remainingBytes(), is(0));
        }

        @Test
        public void testSkipBytesBackward() throws Exception {
            sut_.skipBytes(50);
            int skipped = sut_.skipBytes(-7);
            assertThat(skipped, is(-7));
            assertThat(sut_.remainingBytes(), is(7));
        }

        @Test
        public void testSkipBytesBackwardUnder() throws Exception {
            int skipped = sut_.skipBytes(-10);
            assertThat(skipped, is(0));
            assertThat(sut_.remainingBytes(), is(50));
        }

        @Test
        public void testRemainingBytes() throws Exception {
            int r = sut_.remainingBytes();
            assertThat(r, is(50));

            sut_.readLong();
            r = sut_.remainingBytes();
            assertThat(r, is(42));
        }

        @Test
        public void testCapacityBytes() throws Exception {
            assertThat(sut_.capacityBytes(), is(50));
        }

        @Test
        public void testClear() throws Exception {
            sut_.clear();
            assertThat(sut_.remainingBytes(), is(0));
        }

        @Test
        public void testDrainFrom() throws Exception {
            // drain 40 bytes
            byte[] a = new byte[40];
            Arrays.fill(a, (byte) 'a');
            sut_.clear();
            int drained = sut_.drainFrom(new ArrayCodecBuffer(a, 0, a.length));
            assertThat(drained, is(40));
            assertThat(sut_.remainingBytes(), is(40));
            assertThat(sut_.capacityBytes(), is(50));

            // drain 20 bytes
            byte[] b = new byte[20];
            Arrays.fill(b, (byte) 'b');
            drained = sut_.drainFrom(new ArrayCodecBuffer(b, 0, b.length));
            assertThat(drained, is(20));
            assertThat(sut_.remainingBytes(), is(60));
            assertThat(sut_.capacityBytes(), is(80)); // twice of the remaining

            // drain 150 bytes
            byte[] c = new byte[150];
            Arrays.fill(c, (byte) 'c');
            drained = sut_.drainFrom(new ArrayCodecBuffer(c, 0, c.length));
            assertThat(drained, is(150));
            assertThat(sut_.remainingBytes(), is(210));
            assertThat(sut_.capacityBytes(), is(210)); // required size

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
            CodecBuffer input = new ArrayCodecBuffer(a, 0, a.length);
            sut_.clear();

            // drain 30 bytes.
            int drained = sut_.drainFrom(input, 30);
            assertThat(drained, is(30));
            assertThat(sut_.remainingBytes(), is(30));
            assertThat(sut_.spaceBytes(), is(20));

            // try to drain 20 bytes, but drain 10 bytes
            drained = sut_.drainFrom(input, 20);
            assertThat(drained, is(10));
            assertThat(sut_.remainingBytes(), is(40));
            assertThat(sut_.spaceBytes(), is(10));
        }

        @Test
        public void testDrainFromWithNegative() throws Exception {
            exceptionRule_.expect(IllegalArgumentException.class);
            sut_.drainFrom(Buffers.newCodecBuffer(1), -1);
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
            assertThat(new ArrayCodecBuffer(new byte[1], 1, 0).arrayOffset(), is(0));
        }

        @Test
        public void testSlice() throws Exception {
            byte[] buffer = new byte[3];

            CodecBuffer sliced = sut_.slice(3);

            sliced.readBytes(buffer, 0, buffer.length);
            assertThat(buffer, is(new byte[]{'0', '1', '2'}));
            assertThat(sliced.remainingBytes(), is(0));
            assertThat(sut_.remainingBytes(), is(47));
        }

        @Test
        public void testSliceShare() throws Exception {
            byte[] buffer = new byte[5];
            CodecBuffer input = Buffers.newCodecBuffer(new byte[]{'a', 'b'}, 0, 2);

            CodecBuffer sliced = sut_.slice(3);
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
            sut_.slice(51);
        }

        @Test
        public void testIndexOf_SingleByteHit() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.indexOf('0', 0);
            assertThat(i, is(9));
        }

        @Test
        public void testIndexOf_SingleByteAtTailHit() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.indexOf('9', 48);
            assertThat(i, is(48));
        }

        @Test
        public void testIndexOf_SingleByteNotHit() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.indexOf('0', 48);
            assertThat(i, is(-1));
        }

        @Test
        public void testIndexOf_SingleByteOutOfBound() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.indexOf('9', 49);
            assertThat(i, is(-1));
        }

        @Test
        public void testLastIndexOf_SingleByteHit() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.lastIndexOf('9', 10);
            assertThat(i, is(8));
        }

        @Test
        public void testLastIndexOf_SingleByteAtHeadHit() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.lastIndexOf('1', 0);
            assertThat(i, is(0));
        }

        @Test
        public void testLastIndexOf_SingleByteNotHit() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.lastIndexOf('0', 0);
            assertThat(i, is(-1));
        }

        @Test
        public void testLastIndexOf_SingleByteOutOfBound() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.lastIndexOf('0', -1);
            assertThat(i, is(-1));
        }

        @Test
        public void testIndexOf_MultiByteFromHit() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.indexOf(new byte[]{'9', '0'}, 10);
            assertThat(i, is(18));
        }

        @Test
        public void testIndexOf_MultiByteAtTailHit() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.indexOf(new byte[]{'8', '9'}, 47);
            assertThat(i, is(47));
        }

        @Test
        public void testIndexOf_MultiByteFromNotHit() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.indexOf(new byte[]{'9', '0'}, 47);
            assertThat(i, is(-1));
        }

        @Test
        public void testIndexOf_MultiByteFromTailButTargetIsOutOfBound() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.indexOf(new byte[]{'9', '0'}, 48);
            assertThat(i, is(-1));
        }

        @Test
        public void testIndexOf_MultiByteFromOutOfBound() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.indexOf(new byte[]{'0', '1'}, 49);
            assertThat(i, is(-1));
        }

        @Test
        public void testLastIndexOf_MultiByteHit() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.lastIndexOf(new byte[]{'8', '9'}, 47);
            assertThat(i, is(47));
        }

        @Test
        public void testLastIndexOf_MultiByteAtHeadHit() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.lastIndexOf(new byte[]{'1', '2'}, 9);
            assertThat(i, is(0));
        }

        @Test
        public void testLastIndexOf_MultiByteAtNotHit() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.lastIndexOf(new byte[]{'0', '1'}, 8);
            assertThat(i, is(-1));
        }

        @Test
        public void testLastIndexOf_MultiByteAtHeadButTargetIsOutOfBound() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.lastIndexOf(new byte[]{'0', '1'}, 1);
            assertThat(i, is(-1));
        }

        @Test
        public void testLastIndexOf_MultiByteFromOutOfBound() throws Exception {
            sut_.readByte(); // increment position
            int i = sut_.lastIndexOf(new byte[]{'0', '1'}, 0);
            assertThat(i, is(-1));
        }

    }

    public static abstract class AbstractWriteTests {

        private CodecBuffer sut_;

        @Rule
        public ExpectedException exceptionRule_ = ExpectedException.none();

        protected abstract CodecBuffer createDefaultCodecBuffer();
        protected abstract CodecBuffer createCodecBuffer(int initialCapacity);

        @Before
        public void setUp() {
            sut_ = createCodecBuffer(8);
        }

        @Test
        public void testConstructorSize() throws Exception {
            CodecBuffer localSut = createCodecBuffer(7);
            assertThat(localSut.capacityBytes(), is(7));
        }

        @Test
        public void testConstructorDefault() throws Exception {
            CodecBuffer localSut = createDefaultCodecBuffer();
            assertThat(localSut.capacityBytes(), is(512));
        }

        @Test
        public void testWriteByte() throws Exception {
            sut_.writeByte(10);
            assertThat(sut_.toArray()[0], is((byte) 10));

            sut_.writeByte(20);
            assertThat(sut_.toArray()[1], is((byte) 20));
        }

        @Test
        public void testWriteBytesExpand() throws Exception {
            for (int i = 0; i < 9; i++) {
                sut_.writeByte(0);
            }
            assertThat(sut_.remainingBytes(), is(9));
            assertThat(sut_.capacityBytes(), is(16));
        }

        @Test
        public void testWriteBytesWhole() throws Exception {
            byte[] b = new byte[] {'0', '1', '2'};
            sut_.writeBytes(b, 0, 3);
            assertThat(sut_.remainingBytes(), is(3));
            assertThat(sut_.toArray()[0], is((byte) 0x30));
            assertThat(sut_.toArray()[1], is((byte) 0x31));
            assertThat(sut_.toArray()[2], is((byte) 0x32));
        }

        @Test
        public void testWriteBytesWholeExpand() throws Exception {
            byte[] b = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
            sut_.writeBytes(b, 0, 10);
            assertThat(sut_.remainingBytes(), is(10));
            assertThat(sut_.toArray()[0], is((byte) 0x30));
            assertThat(sut_.toArray()[1], is((byte) 0x31));
            assertThat(sut_.toArray()[8], is((byte) 0x38));
            assertThat(sut_.toArray()[9], is((byte) 0x39));
        }

        @Test
        public void testWriteBytesHead() throws Exception {
            byte[] b = new byte[] {'0', '1', '2'};
            sut_.writeBytes(b, 0, 2);
            assertThat(sut_.remainingBytes(), is(2));
            assertThat(sut_.toArray()[0], is((byte) 0x30));
            assertThat(sut_.toArray()[1], is((byte) 0x31));
        }

        @Test
        public void testWriteBytesHeadExpand() throws Exception {
            byte[] b = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
            sut_.writeBytes(b, 0, 9);
            assertThat(sut_.remainingBytes(), is(9));
            assertThat(sut_.toArray()[0], is((byte) 0x30));
            assertThat(sut_.toArray()[1], is((byte) 0x31));
            assertThat(sut_.toArray()[7], is((byte) 0x37));
            assertThat(sut_.toArray()[8], is((byte) 0x38));
        }

        @Test
        public void testWriteBytesTail() throws Exception {
            byte[] b = new byte[] {'0', '1', '2'};
            sut_.writeBytes(b, 1, 2);
            assertThat(sut_.remainingBytes(), is(2));
            assertThat(sut_.toArray()[0], is((byte) 0x31));
            assertThat(sut_.toArray()[1], is((byte) 0x32));
        }

        @Test
        public void testWriteBytesTailExpand() throws Exception {
            byte[] b = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
            sut_.writeBytes(b, 1, 9);
            assertThat(sut_.remainingBytes(), is(9));
            assertThat(sut_.toArray()[0], is((byte) 0x31));
            assertThat(sut_.toArray()[1], is((byte) 0x32));
            assertThat(sut_.toArray()[7], is((byte) 0x38));
            assertThat(sut_.toArray()[8], is((byte) 0x39));
        }

        @Test
        public void testWriteBytesByteBuffer() throws Exception {
            ByteBuffer bb = ByteBuffer.wrap(new byte[]{'0', '1', '2'});
            sut_.writeBytes(bb);
            assertThat(sut_.remainingBytes(), is(3));
            assertThat(sut_.toArray()[0], is((byte) 0x30));
            assertThat(sut_.toArray()[1], is((byte) 0x31));
            assertThat(sut_.toArray()[2], is((byte) 0x32));
        }

        @Test
        public void testWriteBytesByteBufferExpand() throws Exception {
            ByteBuffer bb = ByteBuffer.wrap(new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
            sut_.writeBytes(bb);
            assertThat(sut_.remainingBytes(), is(10));
            assertThat(sut_.toArray()[0], is((byte) 0x30));
            assertThat(sut_.toArray()[1], is((byte) 0x31));
            assertThat(sut_.toArray()[8], is((byte) 0x38));
            assertThat(sut_.toArray()[9], is((byte) 0x39));
        }

        @Test
        public void testWriteShort() throws Exception {
            sut_.writeShort(Short.MAX_VALUE);
            byte[] b = sut_.toArray();
            assertThat(b[0], is((byte) 0x7F));
            assertThat(b[1], is((byte) 0xFF));
            assertThat(sut_.remainingBytes(), is(2));
        }

        @Test
        public void testWriteChar() throws Exception {
            sut_.writeChar(Character.MAX_VALUE);
            byte[] b = sut_.toArray();
            assertThat(b[0], is((byte) 0xFF));
            assertThat(b[1], is((byte) 0xFF));
            assertThat(sut_.remainingBytes(), is(2));
        }

        @Test
        public void testWriteInt() throws Exception {
            sut_.writeInt(Integer.MIN_VALUE);
            byte[] b = sut_.toArray();
            assertThat(b[0], is((byte) 0x80));
            assertThat(b[1], is((byte) 0x00));
            assertThat(b[2], is((byte) 0x00));
            assertThat(b[3], is((byte) 0x00));
            assertThat(sut_.remainingBytes(), is(4));
        }

        @Test
        public void testWriteLong() throws Exception {
            sut_.writeLong(1);
            byte[] b = sut_.toArray();
            assertThat(b[0], is((byte) 0x00));
            assertThat(b[1], is((byte) 0x00));
            assertThat(b[2], is((byte) 0x00));
            assertThat(b[3], is((byte) 0x00));
            assertThat(b[4], is((byte) 0x00));
            assertThat(b[5], is((byte) 0x00));
            assertThat(b[6], is((byte) 0x00));
            assertThat(b[7], is((byte) 0x01));
            assertThat(sut_.remainingBytes(), is(8));
        }

        @Test
        public void testWriteFloat() throws Exception {
            sut_.writeFloat(1f);
            byte[] b = sut_.toArray();
            assertThat(b[0], is((byte) 0x3F));
            assertThat(b[1], is((byte) 0x80));
            assertThat(b[2], is((byte) 0x00));
            assertThat(b[3], is((byte) 0x00));
            assertThat(sut_.remainingBytes(), is(4));
        }

        @Test
        public void testWriteDouble() throws Exception {
            sut_.writeDouble(1d);
            byte[] b = sut_.toArray();
            assertThat(b[0], is((byte) 0x3F));
            assertThat(b[1], is((byte) 0xF0));
            assertThat(b[2], is((byte) 0x00));
            assertThat(b[3], is((byte) 0x00));
            assertThat(b[4], is((byte) 0x00));
            assertThat(b[5], is((byte) 0x00));
            assertThat(b[6], is((byte) 0x00));
            assertThat(b[7], is((byte) 0x00));
            assertThat(sut_.remainingBytes(), is(8));
        }

        @Test
        public void testWriteString() throws Exception {
            CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
            sut_.writeString(encoder, "abc");
            byte[] b = sut_.toArray();
            assertThat(b[0], is((byte) (CodecUtil.VB_END_BIT | 3)));
            assertThat(b[1], is((byte) 'a'));
            assertThat(b[2], is((byte) 'b'));
            assertThat(b[3], is((byte) 'c'));
        }

        @Test
        public void testWriteStringOverflow() throws Exception {
            CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
            String data = "01234567890123456789あいうえおかきくけこあいうえおかきくけこあいうえお";
            byte[] dataUTF8 = data.getBytes(StandardCharsets.UTF_8);

            // expand internal buffer and byte length estimation failure happen.
            sut_.writeString(encoder, data);

            byte[] b = sut_.toArray();
            assertThat(Arrays.copyOf(b, 2), is(new byte[]{0x1F, (byte) 0x81})); // dataUTF8.length
            for (int i = 0; i < dataUTF8.length; i++) {
                assertThat("index:" + i, b[i + 2], is(dataUTF8[i]));
            }
        }
    }

    public static abstract class AbstractBufferSinkTests {

        private CodecBuffer sut_;
        private int dataLength_;

        protected abstract CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length);

        @Before
        public void setUp() {
            byte[] data = new byte[32];
            Arrays.fill(data, (byte) '0');
            sut_ = createCodecBuffer(data, 0, data.length);
            dataLength_ = data.length;
        }

        @Test
        public void testTransferTo_WriteOnce() throws Exception {
            GatheringByteChannel channel = mock(GatheringByteChannel.class);
            when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    ByteBuffer bb = (ByteBuffer) args[0];
                    bb.position(bb.limit());
                    return bb.limit();
                }
            });

            assertThat(sut_.remainingBytes(), is(dataLength_));

            boolean result = sut_.transferTo(channel);

            assertThat(result, is(true));
            assertThat(sut_.remainingBytes(), is(0));
            verify(channel, times(1)).write(Mockito.any(ByteBuffer.class));
        }

        @Test
        public void testTransferTo_NotEnoughWrite() throws Exception {
            GatheringByteChannel channel = mock(GatheringByteChannel.class);
            when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    ByteBuffer bb = (ByteBuffer) args[0];
                    bb.position(bb.limit() - 1);
                    return dataLength_ - 1;
                }
            });

            assertThat(sut_.remainingBytes(), is(dataLength_));

            boolean result = sut_.transferTo(channel);

            assertThat(result, is(false));
            assertThat(sut_.remainingBytes(), is(1));
            verify(channel, times(1)).write(Mockito.any(ByteBuffer.class));
        }

        @Test(expected = IOException.class)
        public void testTransferTo_IOException() throws  Exception {
            GatheringByteChannel channel = mock(GatheringByteChannel.class);
            when(channel.write(Mockito.any(ByteBuffer.class))).thenThrow(new IOException());

            sut_.transferTo(channel);
            verify(channel, times(1)).write(Mockito.any(ByteBuffer.class));
        }
    }

    public static abstract class AbstractStructureChangeTests {

        protected abstract CodecBuffer createCodecBuffer(byte[] data, int offset, int length);
        protected abstract CodecBuffer createDirectCodecBuffer(ByteBuffer directBuffer);

        @Test
        public void testEnsureSpace_BasedOnRemaining() throws Exception {
            CodecBuffer sut = createCodecBuffer(new byte[10], 7, 3);

            sut.writeBytes(new byte[1], 0, 1);

            assertThat(sut.arrayOffset(), is(0));
            assertThat(sut.toArray().length, is(3 * 2));
            assertThat(sut.remainingBytes(), is(4));
        }

        @Test
        public void testEnsureSpace_BasedOnRequired() throws Exception {
            CodecBuffer sut = createCodecBuffer(new byte[10], 7, 3);

            sut.writeBytes(new byte[4], 0, 4);

            assertThat(sut.arrayOffset(), is(0));
            assertThat(sut.toArray().length, is(7));
            assertThat(sut.remainingBytes(), is(3 + 4));
        }

        @Test
        public void testAddFirst_HasEnoughSpaceAtHead() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[8];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 8, initial.length);
            CodecBuffer sut = createCodecBuffer(data, 8, 8);
            byte[] addedData = new byte[8];
            Arrays.fill(addedData, (byte) 1);
            CodecBuffer added = createCodecBuffer(addedData, 0, addedData.length);

            sut.addFirst(added);

            assertThat(sut.remainingBytes(), is(16));
            byte[] b = new byte[8];
            sut.readBytes(b, 0, 8);
            assertThat(b, is(addedData));
            sut.readBytes(b, 0, 8);
            assertThat(b, is(initial));
            assertThat(sut.arrayOffset(), is(0));
            assertThat(sut.toArray().length, is(16));
        }

        @Test
        public void testAddFirst_HasEnoughSpaceAtHead_HasNoArray() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[8];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 8, initial.length);
            CodecBuffer sut = createCodecBuffer(data, 8, 8);
            byte[] addedData = new byte[8];
            Arrays.fill(addedData, (byte) 1);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8);
            byteBuffer.put(addedData).flip();
            CodecBuffer added = createDirectCodecBuffer(byteBuffer);

            sut.addFirst(added);

            assertThat(sut.remainingBytes(), is(16));
            byte[] b = new byte[8];
            sut.readBytes(b, 0, 8);
            assertThat(b, is(addedData));
            sut.readBytes(b, 0, 8);
            assertThat(b, is(initial));
            assertThat(sut.arrayOffset(), is(0));
            assertThat(sut.toArray().length, is(16));
        }

        @Test
        public void testAddFirst_HasEnoughSpaceAtHeadAndTail() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[8];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 6, initial.length);
            CodecBuffer sut = createCodecBuffer(data, 6, 8);
            byte[] addedData = new byte[8];
            Arrays.fill(addedData, (byte) 1);
            CodecBuffer added = createCodecBuffer(addedData, 0, addedData.length);

            sut.addFirst(added);

            assertThat(sut.remainingBytes(), is(16));
            byte[] b = new byte[8];
            sut.readBytes(b, 0, 8);
            assertThat(b, is(addedData));
            sut.readBytes(b, 0, 8);
            assertThat(b, is(initial));
            assertThat(sut.arrayOffset(), is(0));
            assertThat(sut.toArray().length, is(16));
        }

        @Test
        public void testAddFirst_DoesNotHaveEnoughSpaceSoExpandBufferBasedOnRemaining() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[10];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 6, initial.length);
            CodecBuffer sut = createCodecBuffer(data, 6, 10);
            byte[] addedData = new byte[8];
            Arrays.fill(addedData, (byte) 1);
            CodecBuffer added = createCodecBuffer(addedData, 0, addedData.length);

            sut.addFirst(added);

            assertThat(sut.remainingBytes(), is(18));
            byte[] b8 = new byte[8];
            sut.readBytes(b8, 0, b8.length);
            assertThat(b8, is(addedData));
            byte[] b10 = new byte[10];
            sut.readBytes(b10, 0, b10.length);
            assertThat(b10, is(initial));
            assertThat(sut.arrayOffset(), is(0));
            assertThat(sut.toArray().length, is(10 * 2));
        }

        @Test
        public void testAddFirst_DoesNotHaveEnoughSpaceSoExpandBufferBasedOnRequired() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[10];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 6, initial.length);
            CodecBuffer sut = createCodecBuffer(data, 6, 10);
            byte[] addedData = new byte[11];
            Arrays.fill(addedData, (byte) 1);
            CodecBuffer added = createCodecBuffer(addedData, 0, addedData.length);

            sut.addFirst(added);

            assertThat(sut.remainingBytes(), is(21));
            byte[] b11 = new byte[11];
            sut.readBytes(b11, 0, b11.length);
            assertThat(b11, is(addedData));
            byte[] b10 = new byte[10];
            sut.readBytes(b10, 0, 10);
            assertThat(b10, is(initial));
            assertThat(sut.arrayOffset(), is(0));
            assertThat(sut.toArray().length, is(10 + 11));
        }

        @Test
        public void testAddLast_HasEnoughSpace_HasArray() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[7];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 1, initial.length);
            CodecBuffer sut = createCodecBuffer(data, 1, 7);
            byte[] addedBytes = new byte[8];
            Arrays.fill(addedBytes, (byte) 1);
            CodecBuffer added = createCodecBuffer(addedBytes, 0, 8);

            sut.addLast(added);

            assertThat(sut.remainingBytes(), is(15));
            byte[] b7 = new byte[7];
            sut.readBytes(b7, 0, b7.length);
            assertThat(b7, is(initial));
            byte[] b8 = new byte[8];
            sut.readBytes(b8, 0, b8.length);
            assertThat(b8, is(addedBytes));
            assertThat(sut.arrayOffset(), is(0));
            assertThat(sut.toArray().length, is(16));

        }

        @Test
        public void testAddLast_HasEnoughSpace_HasNoArray() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[7];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 1, initial.length);
            CodecBuffer sut = createCodecBuffer(data, 1, 7);
            byte[] addedBytes = new byte[8];
            Arrays.fill(addedBytes, (byte) 1);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8);
            byteBuffer.put(addedBytes).flip();
            CodecBuffer added = createDirectCodecBuffer(byteBuffer);

            sut.addLast(added);

            assertThat(sut.remainingBytes(), is(15));
            byte[] b7 = new byte[7];
            sut.readBytes(b7, 0, b7.length);
            assertThat(b7, is(initial)); // all zero
            byte[] b8 = new byte[8];
            sut.readBytes(b8, 0, b8.length);
            assertThat(b8, is(addedBytes));
            assertThat(sut.arrayOffset(), is(0));
            assertThat(sut.toArray().length, is(16));
        }

        @Test
        public void testAddLast_HasEnoughSpaceAtHeadAndTail() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[9];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 0, initial.length);
            CodecBuffer sut = createCodecBuffer(data, 0, 9);
            sut.readBytes(new byte[2], 0, 2); // left 7 bytes, front space 2 bytes
            byte[] addedBytes = new byte[8];
            Arrays.fill(addedBytes, (byte) 1);
            CodecBuffer added = createCodecBuffer(addedBytes, 0, addedBytes.length);

            sut.addLast(added);

            assertThat(sut.remainingBytes(), is(15));
            byte[] firstHalf = new byte[7];
            sut.readBytes(firstHalf, 0, firstHalf.length);
            assertThat(firstHalf, is(Arrays.copyOf(initial, 7)));
            byte[] secondHalf = new byte[8];
            sut.readBytes(secondHalf, 0, secondHalf.length);
            assertThat(secondHalf, is(addedBytes));
            assertThat(sut.arrayOffset(), is(0));
            assertThat(sut.toArray().length, is(data.length));
        }

        @Test
        public void testAddLast_DoseNotHaveEnoughSpaceSoExpandBasedOnRemaining() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[8];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 2, initial.length);
            CodecBuffer sut = createCodecBuffer(data, 2, 8);
            byte[] addedBytes = new byte[7];
            Arrays.fill(addedBytes, (byte) 1);
            CodecBuffer added = createCodecBuffer(addedBytes, 0, addedBytes.length);

            sut.addLast(added);

            assertThat(sut.remainingBytes(), is(15));
            byte[] firstHalf = new byte[8];
            sut.readBytes(firstHalf, 0, firstHalf.length);
            assertThat(firstHalf, is(initial));
            byte[] secondHalf = new byte[7];
            sut.readBytes(secondHalf, 0, secondHalf.length);
            assertThat(secondHalf, is(addedBytes));
            assertThat(sut.arrayOffset(), is(0));
            assertThat(sut.toArray().length, is(8 * 2));
        }

        @Test
        public void testAddLast_DoseNotHaveEnoughSpaceSoExpandBasedOnRequired() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[8];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 2, initial.length);
            CodecBuffer sut = createCodecBuffer(data, 2, 8);
            byte[] addedBytes = new byte[9];
            Arrays.fill(addedBytes, (byte) 1);
            CodecBuffer added = createCodecBuffer(addedBytes, 0, addedBytes.length);

            sut.addLast(added);

            assertThat(sut.remainingBytes(), is(17));
            byte[] firstHalf = new byte[8];
            sut.readBytes(firstHalf, 0, firstHalf.length);
            assertThat(firstHalf, is(initial));
            byte[] secondHalf = new byte[9];
            sut.readBytes(secondHalf, 0, secondHalf.length);
            assertThat(secondHalf, is(addedBytes));
            assertThat(sut.arrayOffset(), is(0));
            assertThat(sut.toArray().length, is(8 + 9));
        }
    }
}
