package net.ihiroky.niotty.buffer;

import net.ihiroky.niotty.util.Charsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
@RunWith(Enclosed.class)
public class CodecBufferListTest {

    private static final Charset CHARSET = Charsets.UTF_8;

    public static class EmptyCase extends CodecBufferTestAbstract.AbstractEmptyTests {
        @Override
        protected CodecBuffer createCodecBuffer() {
            return new CodecBufferList(Buffers.newCodecBuffer());
        }
    }

    public static class ReadTests extends CodecBufferTestAbstract.AbstractReadTests {
        @Override
        protected CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length) {
            return new CodecBufferList(Buffers.wrap(buffer, offset, length));
        }

        @Test
        public void testReadBytes_ArrayBetweenBuffers() throws Exception {
            byte[] data0 = new byte[]{'0', '0', '0'};
            byte[] data1 = new byte[]{'1', '1', '1'};
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length),
                    Buffers.wrap(data1, 0, data1.length));

            byte[] actualData = new byte[6];
            int actualRead = sut.readBytes(actualData, 0, actualData.length);

            assertThat(actualRead, is(6));
            assertThat(actualData, is(new byte[] {'0', '0', '0', '1', '1', '1'}));
            assertThat(sut.remainingBytes(), is(0));
            assertThat(sut.beginningBufferIndex(), is(1));
            assertThat(sut.endBufferIndex(), is(1));
            assertThat(sut.sizeOfBuffers(), is(2));
        }

        @Test
        public void testReadBytes_ByteBufferBetweenBuffers() throws Exception {
            byte[] data0 = new byte[]{'0', '0', '0'};
            byte[] data1 = new byte[]{'1', '1', '1'};
            CodecBufferList sut = new CodecBufferList( 
                    Buffers.wrap(ByteBuffer.wrap(data0, 0, data0.length)),
                    Buffers.wrap(ByteBuffer.wrap(data1, 0, data1.length)));

            byte[] actualData = new byte[6];
            int actualRead = sut.readBytes(actualData, 0, actualData.length);

            assertThat(actualRead, is(6));
            assertThat(actualData, is(new byte[] {'0', '0', '0', '1', '1', '1'}));
            assertThat(sut.remainingBytes(), is(0));
            assertThat(sut.beginningBufferIndex(), is(1));
            assertThat(sut.endBufferIndex(), is(1));
            assertThat(sut.sizeOfBuffers(), is(2));
        }

        @Test
        public void testReadChar_BetweenBuffers() throws Exception {
            char c = 'い';
            byte[] data = new byte[]{(byte) (c >> 8), (byte) (c & 0xFF)};
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data, 0, 1),
                    Buffers.wrap(data, 1, 1));

            char actual = sut.readChar();

            assertThat(actual, is(c));
            assertThat(sut.remainingBytes(), is(0));
            assertThat(sut.beginningBufferIndex(), is(1));
            assertThat(sut.endBufferIndex(), is(1));
            assertThat(sut.sizeOfBuffers(), is(2));
        }

        @Test
        public void testReadShort_BetweenBuffers() throws Exception {
            short s = Short.MAX_VALUE;
            byte[] data = new byte[]{(byte) (s >> 8), (byte) (s & 0xFF)};
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data, 0, 1),
                    Buffers.wrap(data, 1, 1));

            short actual = sut.readShort();

            assertThat(actual, is(s));
            assertThat(sut.remainingBytes(), is(0));
            assertThat(sut.beginningBufferIndex(), is(1));
            assertThat(sut.endBufferIndex(), is(1));
            assertThat(sut.sizeOfBuffers(), is(2));
        }

        @Test
        public void testReadInt_BetweenBuffers() throws Exception {
            int i = Integer.MAX_VALUE;
            byte[] data = new byte[]{
                    (byte) (i >>> 24), (byte) ((i >>> 16) | 0xFF), (byte) ((i >>> 8) | 0xFF), (byte) (i & 0xFF)};
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data, 0, 3), Buffers.wrap(data, 1, 1));

            int actual = sut.readInt();

            assertThat(actual, is(i));
            assertThat(sut.remainingBytes(), is(0));
            assertThat(sut.beginningBufferIndex(), is(1));
            assertThat(sut.endBufferIndex(), is(1));
            assertThat(sut.sizeOfBuffers(), is(2));
        }

        @Test
        public void testReadLong_BetweenBuffers() throws Exception {
            long v = Long.MAX_VALUE;
            byte[] data = new byte[]{
                    (byte) (v >>> 56), (byte) ((v >>> 48) | 0xFF), (byte) ((v >>> 40) | 0xFF),
                    (byte) ((v >>> 32) | 0xFF), (byte) ((v >>> 24) | 0xFF), (byte) ((v >>> 16) | 0xFF),
                    (byte) ((v >>> 8) | 0xFF), (byte) (v & 0xFF)};
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data, 0, 7),
                    Buffers.wrap(data, 1, 1));

            long actual = sut.readLong();

            assertThat(actual, is(v));
            assertThat(sut.remainingBytes(), is(0));
            assertThat(sut.beginningBufferIndex(), is(1));
            assertThat(sut.endBufferIndex(), is(1));
            assertThat(sut.sizeOfBuffers(), is(2));
        }

        @Test
        public void testReadString_BetweenBuffers() throws Exception {
            String s = "あい0123";
            byte[] data = s.getBytes("UTF-8");
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data, 0, 4),
                    Buffers.wrap(data, 4, 4),
                    Buffers.wrap(data, 8, 2));

            String actual = sut.readString(CHARSET.newDecoder(), data.length);

            assertThat(actual, is(s));
            assertThat(sut.remainingBytes(), is(0));
            assertThat(sut.beginningBufferIndex(), is(2));
            assertThat(sut.endBufferIndex(), is(2));
            assertThat(sut.sizeOfBuffers(), is(3));
        }

        @Test
        public void testSkipBytes_BetweenBuffers() throws Exception {
            byte[] data0 = new byte[5];
            Arrays.fill(data0, (byte) 1);
            byte[] data1 = new byte[5];
            Arrays.fill(data1, (byte) 2);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, 5),
                    Buffers.wrap(data1, 0, 5));

            int first = sut.skipBytes(8);
            int firstBeginning = sut.beginning();
            int firstBeginningIndex = sut.beginningBufferIndex();

            int second = sut.skipBytes(-4);
            int secondBeginning = sut.beginning();
            int secondBeginningIndex = sut.beginningBufferIndex();

            int third = sut.skipBytes(7); // over skip to positive side
            int thirdBeginning = sut.beginning();
            int thirdBeginningIndex = sut.beginningBufferIndex();

            int forth = sut.skipBytes(-11); // over skip to negative side
            int forthBeginning = sut.beginning();
            int forthBeginningIndex = sut.beginningBufferIndex();

            // at second buffer
            assertThat(first, is(8));
            assertThat(firstBeginning, is(8));
            assertThat(firstBeginningIndex, is(1));
            // at first buffer
            assertThat(second, is(-4));
            assertThat(secondBeginning, is(4));
            assertThat(secondBeginningIndex, is(0));
            // at end of second buffer
            assertThat(third, is(6));
            assertThat(thirdBeginning, is(10));
            assertThat(thirdBeginningIndex, is(1));
            // at beginning of the first buffer
            assertThat(forth, is(-10));
            assertThat(forthBeginning, is(0));
            assertThat(forthBeginningIndex, is(0));
        }

        @Test
        public void testSlice_BetweenBuffers() throws Exception {
            byte[] data0 = new byte[3];
            Arrays.fill(data0, (byte) 1);
            byte[] data1 = new byte[3];
            Arrays.fill(data1, (byte) 2);
            byte[] data2 = new byte[3];
            Arrays.fill(data2, (byte) 3);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length),
                    Buffers.wrap(data1, 0, data1.length),
                    Buffers.wrap(data2, 0, data2.length));

            CodecBuffer actual = sut.slice(8);

            assertThat(actual.remainingBytes(), is(8));
            assertThat(sut.remainingBytes(), is(1));
            assertThat(sut.beginningBufferIndex(), is(2));
            assertThat(sut.endBufferIndex(), is(2));
            byte[] actualBytes = new byte[actual.remainingBytes()];
            actual.readBytes(actualBytes, 0, actualBytes.length);
            assertThat(actualBytes, is(new byte[]{1, 1, 1, 2, 2, 2, 3, 3}));
        }

        @Test
        public void testSlice_FromHeadOnly() throws Exception {
            byte[] data0 = new byte[3];
            Arrays.fill(data0, (byte) 1);
            byte[] data1 = new byte[3];
            Arrays.fill(data1, (byte) 2);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length),
                    Buffers.wrap(data1, 0, data1.length));

            CodecBuffer actual = sut.slice(2);

            assertThat(actual.remainingBytes(), is(2));
            assertThat(sut.remainingBytes(), is(4));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(1));
            byte[] actualBytes = new byte[actual.remainingBytes()];
            actual.readBytes(actualBytes, 0, actualBytes.length);
            assertThat(actualBytes, is(new byte[]{1, 1}));
        }

        @Test
        public void testClear() throws Exception {
            CodecBuffer b0 = Buffers.wrap(new byte[3], 1, 1);
            CodecBuffer b1 = Buffers.wrap(new byte[3], 1, 1);
            CodecBufferList sut = new CodecBufferList( b0, b1);

            sut.clear();

            assertThat(sut.remainingBytes(), is(0));
            assertThat(sut.beginning(), is(0));
            assertThat(sut.end(), is(0));
            assertThat(sut.capacityBytes(), is(1 + 1));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(0));
            assertThat(b0.remainingBytes(), is(0)); // effected
            assertThat(b1.remainingBytes(), is(0)); // effected
        }
    }

    public static class UnsignedTests extends CodecBufferTestAbstract.AbstractUnsignedTest {
        @Override
        protected CodecBuffer createCodecBuffer(byte[] buffer, int offset, int length) {
            return new CodecBufferList(Buffers.wrap(buffer, offset, length));
        }
    }


    public static class WriteTests {
        private void assertData(CodecBuffer buffer, byte[] expected) {
            byte[] actual = new byte[expected.length];
            buffer.readBytes(actual, 0, actual.length);
            assertThat(actual, is(expected));
        }

        @Test
        public void testWriteBytes_ArrayBetweenCodecBuffer() throws Exception {
            byte[] data0 = new byte[4];
            Arrays.fill(data0, (byte) 1);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length));
            byte[] data1 = new byte[6];
            Arrays.fill(data1, (byte) 2);
            byte[] data2 = new byte[4];
            Arrays.fill(data2, (byte) 3);

            sut.writeBytes(data1, 0, data1.length); // add new buffer. capacity gets 4 + 8
            sut.writeBytes(data2, 0, data2.length); // add new buffer. capacity gets 4 + 8 + 16

            assertThat(sut.remainingBytes(), is(4 + 6 + 4));
            assertThat(sut.capacityBytes(), is(4 + 8 + 16));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(2));
            assertThat(sut.sizeOfBuffers(), is(3));
            assertData(sut, data0);
            assertData(sut, data1);
            assertData(sut, data2);
        }

        @Test
        public void testWriteBytes_ArrayNoExpansion() throws Exception {
            byte[] data0 = new byte[4];
            Arrays.fill(data0, (byte) 1);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length));
            byte[] data1 = new byte[2];
            Arrays.fill(data1, (byte) 2);
            byte[] data2 = new byte[6];
            Arrays.fill(data2, (byte) 3);

            sut.writeBytes(data1, 0, data1.length); // capacity: 4 + 8
            sut.writeBytes(data2, 0, data2.length); // no expansion

            assertThat(sut.remainingBytes(), is(4 + 2 + 6));
            assertThat(sut.capacityBytes(), is(4 + 8));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(1));
            assertData(sut, data0);
            assertData(sut, data1);
            assertData(sut, data2);
        }

        @Test
        public void testWriteBytes_ByteBufferBetweenCodecBuffer() throws Exception {
            byte[] data0 = new byte[4];
            Arrays.fill(data0, (byte) 1);
            CodecBufferList sut = new CodecBufferList(Buffers.wrap(data0, 0, data0.length));
            byte[] data1 = new byte[6];
            Arrays.fill(data1, (byte) 2);
            byte[] data2 = new byte[4];
            Arrays.fill(data2, (byte) 3);

            sut.writeBytes(ByteBuffer.wrap(data1, 0, data1.length)); // add new buffer. capacity gets 4 + 8
            sut.writeBytes(ByteBuffer.wrap(data2, 0, data2.length)); // add new buffer. capacity gets 4 + 8 + 16

            assertThat(sut.remainingBytes(), is(4 + 6 + 4));
            assertThat(sut.capacityBytes(), is(4 + 8 + 16));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(2));
            assertThat(sut.sizeOfBuffers(), is(3));
            byte[] actual0 = new byte[4];
            sut.readBytes(actual0, 0, actual0.length);
            assertThat(actual0, is(data0));
            byte[] actual1 = new byte[6];
            sut.readBytes(actual1, 0, actual1.length);
            assertThat(actual1, is(data1));
            byte[] actual2 = new byte[4];
            sut.readBytes(actual2, 0, actual2.length);
            assertThat(actual2, is(data2));
        }

        @Test
        public void testWriteBytes_ByteBufferNoExpansion() throws Exception {
            byte[] data0 = new byte[4];
            Arrays.fill(data0, (byte) 1);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length));
            byte[] data1 = new byte[2];
            Arrays.fill(data1, (byte) 2);
            byte[] data2 = new byte[6];
            Arrays.fill(data2, (byte) 3);

            sut.writeBytes(ByteBuffer.wrap(data1, 0, data1.length)); // capacity: 4 + 8
            sut.writeBytes(ByteBuffer.wrap(data2, 0, data2.length)); // no expansion

            assertThat(sut.remainingBytes(), is(4 + 2 + 6));
            assertThat(sut.capacityBytes(), is(4 + 8));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(1));
            assertData(sut, data0);
            assertData(sut, data1);
            assertData(sut, data2);
        }

        @Test
        public void testWriteShort_BetweenCodecBuffer() throws Exception {
            byte[] data0 = new byte[1];
            Arrays.fill(data0, (byte) 1);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length));

            sut.writeByte(2); // capacity gets 1 + 2
            sut.writeShort((short) -1); // new buffer allocated if one byte space exists

            assertThat(sut.remainingBytes(), is(1 + 1 + 2));
            assertThat(sut.capacityBytes(), is(1 + 2 + 4));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(2));
            assertThat(sut.sizeOfBuffers(), is(3));
            assertData(sut, data0);
            assertData(sut, new byte[]{2});
            assertData(sut, new byte[]{-1, -1});
        }

        @Test
        public void testWriteShort_NoExpansion() throws Exception {
            byte[] data0 = new byte[2];
            Arrays.fill(data0, (byte) 1);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length));

            sut.writeByte(2); // capacity gets 2 + 4
            sut.writeShort((short) -1);

            assertThat(sut.remainingBytes(), is(2 + 1 + 2));
            assertThat(sut.capacityBytes(), is(2 + 4));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(1));
            assertThat(sut.sizeOfBuffers(), is(2));
            assertData(sut, data0);
            assertData(sut, new byte[]{2});
            assertData(sut, new byte[]{-1, -1});
        }

        @Test
        public void testWriteChar_BetweenCodecBuffer() throws Exception {
            byte[] data0 = new byte[1];
            Arrays.fill(data0, (byte) 1);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length));

            sut.writeByte(2); // capacity gets 1 + 2
            sut.writeChar(Character.MAX_VALUE); // new buffer allocated if one byte space exists

            assertThat(sut.remainingBytes(), is(1 + 1 + 2));
            assertThat(sut.capacityBytes(), is(1 + 2 + 4));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(2));
            assertThat(sut.sizeOfBuffers(), is(3));
            assertData(sut, data0);
            assertData(sut, new byte[]{2});
            assertData(sut, new byte[]{-1, -1});
        }

        @Test
        public void testWriteChar_NoExpansion() throws Exception {
            byte[] data0 = new byte[2];
            Arrays.fill(data0, (byte) 1);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length));

            sut.writeByte(2); // capacity gets 2 + 4
            sut.writeChar(Character.MAX_VALUE);

            assertThat(sut.remainingBytes(), is(2 + 1 + 2));
            assertThat(sut.capacityBytes(), is(2 + 4));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(1));
            assertThat(sut.sizeOfBuffers(), is(2));
            assertData(sut, data0);
            assertData(sut, new byte[]{2});
            assertData(sut, new byte[]{-1, -1});
        }

        @Test
        public void testWriteInt_BetweenCodecBuffer() throws Exception {
            byte[] data0 = new byte[1];
            Arrays.fill(data0, (byte) 1);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length));

            sut.writeInt(-1);

            assertThat(sut.remainingBytes(), is(1 + 4));
            assertThat(sut.capacityBytes(), is(1 + 2 + 4));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(2));
            assertThat(sut.sizeOfBuffers(), is(3));
            assertData(sut, data0);
            assertData(sut, new byte[]{-1, -1, -1, -1});
        }

        @Test
        public void testWriteInt_NoExpansion() throws Exception {
            byte[] data0 = new byte[3];
            Arrays.fill(data0, (byte) 1);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length));

            sut.writeByte(2); // capacity gets 3 + 6
            sut.writeInt(-1);

            assertThat(sut.remainingBytes(), is(3 + 1 + 4));
            assertThat(sut.capacityBytes(), is(3 + 6));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(1));
            assertThat(sut.sizeOfBuffers(), is(2));
            assertData(sut, data0);
            assertData(sut, new byte[]{2});
            assertData(sut, new byte[]{-1, -1, -1, -1});
        }

        @Test
        public void testWriteLong_BetweenCodecBuffer() throws Exception {
            byte[] data0 = new byte[1];
            Arrays.fill(data0, (byte) 1);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length));

            sut.writeLong(-1L);

            assertThat(sut.remainingBytes(), is(1 + 8));
            assertThat(sut.capacityBytes(), is(1 + 2 + 4 + 8));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(3));
            assertThat(sut.sizeOfBuffers(), is(4));
            assertData(sut, data0);
            assertData(sut, new byte[]{-1, -1, -1, -1, -1, -1, -1, -1});
        }

        @Test
        public void testWriteLong_NoExpansion() throws Exception {
            byte[] data0 = new byte[5];
            Arrays.fill(data0, (byte) 1);
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data0, 0, data0.length));

            sut.writeByte(2); // capacity gets 5 + 10
            sut.writeLong(-1L);

            assertThat(sut.remainingBytes(), is(5 + 1 + 8));
            assertThat(sut.capacityBytes(), is(5 + 10));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(1));
            assertThat(sut.sizeOfBuffers(), is(2));
            assertData(sut, data0);
            assertData(sut, new byte[]{2});
            assertData(sut, new byte[]{-1, -1, -1, -1, -1, -1, -1, -1});
        }

        @Test
        public void testWriteString_BetweenCodecBuffer() throws Exception {
            CodecBufferList sut = new CodecBufferList(
                    Buffers.newCodecBuffer(0));
            String s = "0123456789";

            sut.writeString(s, CHARSET.newEncoder());

            assertThat(sut.remainingBytes(), is(10));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(1));
            assertThat(sut.sizeOfBuffers(), is(2));
            byte[] actual = new byte[sut.remainingBytes()];
            sut.readBytes(actual, 0, actual.length);
            assertThat(actual, is(s.getBytes()));
        }
    }

    public static class BufferSinkTests {

        private CodecBuffer sut_;
        private int dataLength_;

        @Before
        public void setUp() {
            byte[] data = new byte[32];
            Arrays.fill(data, (byte) '0');
            sut_ = new CodecBufferList(Buffers.wrap(data, 0, data.length));
            dataLength_ = data.length;
        }

        @Test
        public void testTransferTo_SingleBufferWriteOnce() throws Exception {
            GatheringByteChannel channel = mock(GatheringByteChannel.class);
            when(channel.write(Mockito.any(ByteBuffer[].class), anyInt(), anyInt())).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    ByteBuffer[] bb = (ByteBuffer[]) args[0];
                    bb[0].position(bb[0].limit());
                    return bb[0].limit();
                }
            });

            assertThat(sut_.remainingBytes(), is(dataLength_));

            boolean result = sut_.transferTo(channel);

            assertThat(result, is(true));
            assertThat(sut_.remainingBytes(), is(0));
            verify(channel, times(1)).write(Mockito.any(ByteBuffer[].class), anyInt(), anyInt());
        }

        @Test
        public void testTransferTo_SingleBufferNotEnoughWrite() throws Exception {
            GatheringByteChannel channel = mock(GatheringByteChannel.class);
            when(channel.write(Mockito.any(ByteBuffer[].class), anyInt(), anyInt())).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    ByteBuffer[] bb = (ByteBuffer[]) args[0];
                    bb[0].position(bb[0].limit() - 1);
                    return dataLength_ - 1;
                }
            });

            assertThat(sut_.remainingBytes(), is(dataLength_));

            boolean result = sut_.transferTo(channel);

            assertThat(result, is(false));
            assertThat(sut_.remainingBytes(), is(1));
            verify(channel, times(1)).write(Mockito.any(ByteBuffer[].class), anyInt(), anyInt());
        }

        @Test(expected = IOException.class)
        public void testTransferTo_SingleBufferIOException() throws  Exception {
            GatheringByteChannel channel = mock(GatheringByteChannel.class);
            when(channel.write(Mockito.any(ByteBuffer[].class), anyInt(), anyInt())).thenThrow(new IOException());

            sut_.transferTo(channel);
            verify(channel, times(1)).write(Mockito.any(ByteBuffer.class));
        }

        @Test
        public void testCopyTo_ByteBufferAll() throws Exception {
            ByteBuffer buffer = ByteBuffer.allocate(sut_.remainingBytes());

            sut_.copyTo(buffer);

            assertThat(sut_.remainingBytes(), is(32)); // remaining all
            assertThat(buffer.array(), is(sut_.array()));
        }

        @Test(expected = BufferOverflowException.class)
        public void testCopyTo_ByteBufferPart() throws Exception {
            ByteBuffer buffer = ByteBuffer.allocate(sut_.remainingBytes() - 1);
            sut_.copyTo(buffer);
        }

        @Test
        public void testDrainFrom_AllBetweenBuffers() throws Exception {
            CodecBufferList sut = new CodecBufferList(
                    Buffers.newCodecBuffer(3), Buffers.newCodecBuffer(3));
            byte[] drainedBytes = new byte[10];
            Arrays.fill(drainedBytes, (byte) 1);
            CodecBuffer drained = Buffers.wrap(drainedBytes, 0, drainedBytes.length);

            int actualBytes = sut.drainFrom(drained);

            assertThat(actualBytes, is(10));
            assertThat(drained.remainingBytes(), is(0));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(2));
            assertThat(sut.sizeOfBuffers(), is(3));
        }

        @Test
        public void testDrainFrom_LimitedBetweenBuffers() throws Exception {
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(new byte[3], 0, 3));
            byte[] drainedBytes = new byte[10];
            Arrays.fill(drainedBytes, (byte) 1);
            CodecBuffer drained = Buffers.wrap(drainedBytes, 0, drainedBytes.length);

            int actualBytes = sut.drainFrom(drained, 6);

            assertThat(actualBytes, is(6));
            assertThat(drained.remainingBytes(), is(4));
            assertThat(sut.remainingBytes(), is(3 + 6));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(1));
            assertThat(sut.sizeOfBuffers(), is(2));
        }

        @Test
        public void testArray_MultipleElements() throws Exception {
            byte[] data = new byte[10];
            Arrays.fill(data, (byte) '0');
            CodecBufferList sut = new CodecBufferList(
                    Buffers.wrap(data, 0, 5), Buffers.wrap(data, 5, 5));

            byte[] array = sut.array();

            assertThat(array, is(data));
            assertThat(array, is(not(sameInstance(data))));
            assertThat(sut.hasArray(), is(false));
        }
    }

    public static class StructureChangeTests  {

        protected CodecBufferList createCodecBuffer(byte[] data, int offset, int length) {
            return new CodecBufferList(Buffers.wrap(data, offset, length));
        }

        protected CodecBuffer createDirectCodecBuffer(ByteBuffer directBuffer) {
            return Buffers.wrap(directBuffer);
        }

        @Test
        public void testEnsureSpace_BasedOnLastCapacity() throws Exception {
            CodecBuffer sut = createCodecBuffer(new byte[10], 7, 3);

            sut.writeBytes(new byte[1], 0, 1);

            assertThat(sut.remainingBytes(), is(4));
            assertThat(sut.capacityBytes(), is(3 + 6));
            assertThat(sut.beginning(), is(0));
            assertThat(sut.end(), is(4));
        }

        @Test
        public void testEnsureSpace_BasedOnRequired() throws Exception {
            CodecBuffer sut = createCodecBuffer(new byte[10], 7, 3);

            sut.writeBytes(new byte[21], 0, 21);

            assertThat(sut.remainingBytes(), is(3 + 21));
            assertThat(sut.capacityBytes(), is(3 + 21));
            assertThat(sut.beginning(), is(0));
            assertThat(sut.end(), is(24));
        }

        @Test
        public void testAddFirst_AtHead() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[8];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 8, initial.length);
            CodecBufferList sut = createCodecBuffer(data, 8, 8);
            byte[] addedData = new byte[8];
            Arrays.fill(addedData, (byte) 1);
            CodecBuffer added = createCodecBuffer(addedData, 0, addedData.length);

            sut.addFirst(added);

            assertThat(sut.remainingBytes(), is(16));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(1));
            byte[] b = new byte[8];
            sut.readBytes(b, 0, 8);
            assertThat(b, is(addedData));
            sut.readBytes(b, 0, 8);
            assertThat(b, is(initial));
        }


        @Test
        public void testAddFirst_EmptyBufferExistsAtHead() throws Exception {
            byte[] initial = new byte[8];
            Arrays.fill(initial, (byte) -1);
            CodecBufferList sut = createCodecBuffer(new byte[0], 0, 0); // first buffer gets empty
            sut.addLast(Buffers.wrap(initial, 0, 8));
            byte[] addedData = new byte[8];
            Arrays.fill(addedData, (byte) 1);
            CodecBuffer added = createCodecBuffer(addedData, 0, addedData.length);

            sut.addFirst(added);

            assertThat(sut.remainingBytes(), is(8 + 8));
            assertThat(sut.beginningBufferIndex(), is(1));
            assertThat(sut.endBufferIndex(), is(2));
            byte[] b = new byte[8];
            sut.readBytes(b, 0, 8);
            assertThat(b, is(addedData));
            sut.readBytes(b, 0, 8);
            assertThat(b, is(initial));
        }

         @Test
        public void testAddFirst_SutIsEmpty() throws Exception {
            CodecBufferList sut = createCodecBuffer(new byte[0], 0, 0);
            CodecBuffer added = Buffers.wrap(new byte[10], 0, 10);

            sut.addFirst(added);

            assertThat(sut.remainingBytes(), is(added.remainingBytes()));
            assertThat(sut.beginningBufferIndex(), is(1));
            assertThat(sut.endBufferIndex(), is(1));
        }

        @Test
        public void testAddLast_AtLast() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[7];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 1, initial.length);
            CodecBufferList sut = createCodecBuffer(data, 1, 7);
            byte[] addedBytes = new byte[8];
            Arrays.fill(addedBytes, (byte) 1);
            CodecBuffer added = createCodecBuffer(addedBytes, 0, 8);

            sut.addLast(added);

            assertThat(sut.remainingBytes(), is(15));
            assertThat(sut.beginningBufferIndex(), is(0));
            assertThat(sut.endBufferIndex(), is(1));
            byte[] b7 = new byte[7];
            sut.readBytes(b7, 0, b7.length);
            assertThat(b7, is(initial));
            byte[] b8 = new byte[8];
            sut.readBytes(b8, 0, b8.length);
            assertThat(b8, is(addedBytes));
        }

        @Test
        public void testAddLast_EmptyBufferExistsAtLast() throws Exception {
            byte[] data = new byte[16];
            byte[] initial = new byte[7];
            Arrays.fill(initial, (byte) -1);
            System.arraycopy(initial, 0, data, 1, initial.length);
            CodecBufferList sut = createCodecBuffer(new byte[0], 0, 0);
            sut.addFirst(Buffers.wrap(data, 1, 7)); // last buffer gets empty
            byte[] addedBytes = new byte[8];
            Arrays.fill(addedBytes, (byte) 1);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8);
            byteBuffer.put(addedBytes).flip();
            CodecBuffer added = createDirectCodecBuffer(byteBuffer);

            sut.addLast(added);

            assertThat(sut.remainingBytes(), is(15));
            assertThat(sut.beginningBufferIndex(), is(1));
            assertThat(sut.endBufferIndex(), is(2));
            byte[] b7 = new byte[7];
            sut.readBytes(b7, 0, b7.length);
            assertThat(b7, is(initial));
            byte[] b8 = new byte[8];
            sut.readBytes(b8, 0, b8.length);
            assertThat(b8, is(addedBytes));
        }

        @Test
        public void testAddLast_SutIsEmpty() throws Exception {
            CodecBufferList sut = createCodecBuffer(new byte[0], 0, 0);
            CodecBuffer added = Buffers.wrap(new byte[10], 0, 10);

            sut.addLast(added);

            assertThat(sut.remainingBytes(), is(added.remainingBytes()));
            assertThat(sut.beginningBufferIndex(), is(0)); // first empty buffer is still visible
            assertThat(sut.endBufferIndex(), is(1));
        }
    }

}
