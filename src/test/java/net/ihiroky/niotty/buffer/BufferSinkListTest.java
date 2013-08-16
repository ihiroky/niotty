package net.ihiroky.niotty.buffer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class BufferSinkListTest {

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Test
    public void testDispose() throws Exception {
        BufferSink car = mock(BufferSink.class);
        doNothing().when(car).dispose();
        BufferSink cdr = mock(BufferSink.class);
        doNothing().when(cdr).dispose();

        BufferSinkList list = new BufferSinkList(car, cdr);
        list.dispose();

        verify(car, times(1)).dispose();
        verify(cdr, times(1)).dispose();
    }

    @Test
    public void testTransferTo_CarAndCdrIsTrue() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        BufferSink car = mock(BufferSink.class);
        doReturn(true).when(car).transferTo(channel);
        BufferSink cdr = mock(BufferSink.class);
        doReturn(true).when(cdr).transferTo(channel);

        BufferSinkList List = new BufferSinkList(car, cdr);
        boolean result = List.transferTo(channel);

        assertThat(result, is(true));
        verify(car, times(1)).transferTo(channel);
        verify(cdr, times(1)).transferTo(channel);
    }

    @Test
    public void testTransferTo_CarIsTrueAndCdrIsFalse() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        BufferSink car = mock(BufferSink.class);
        doReturn(true).when(car).transferTo(channel);
        BufferSink cdr = mock(BufferSink.class);
        doReturn(false).when(cdr).transferTo(channel);

        BufferSinkList List = new BufferSinkList(car, cdr);
        boolean result = List.transferTo(channel);

        assertThat(result, is(false));
        verify(car, times(1)).transferTo(channel);
        verify(cdr, times(1)).transferTo(channel);
    }

    @Test
    public void testTransferTo_CarAndCdrIsFalse() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        BufferSink car = mock(BufferSink.class);
        doReturn(false).when(car).transferTo(channel);
        BufferSink cdr = mock(BufferSink.class);
        doReturn(false).when(cdr).transferTo(channel);

        BufferSinkList sut = new BufferSinkList(car, cdr);
        boolean result = sut.transferTo(channel);

        assertThat(result, is(false));
        verify(car, times(1)).transferTo(channel);
        verify(cdr, never()).transferTo(channel);
    }

    @Test
    public void testTransferTo_WriteAllDataIfByteBufferIsLargeEnough() throws Exception {
        byte[] carData = new byte[]{0};
        BufferSink car = Buffers.wrap(carData, 0, carData.length);
        byte[] cdrData = new byte[]{1, 2};
        BufferSink cdr = Buffers.wrap(cdrData, 0, cdrData.length);
        ByteBuffer buffer = ByteBuffer.allocate(3);

        BufferSinkList sut = new BufferSinkList(car, cdr);
        sut.transferTo(buffer);
        buffer.flip();

        assertThat(buffer, is(ByteBuffer.wrap(new byte[]{0, 1, 2})));
        assertThat(car.remainingBytes(), is(1));
        assertThat(cdr.remainingBytes(), is(2));
    }

    @Test
    public void testAddFirst() throws Exception {
        byte[] b0 = new byte[]{'0'};
        byte[] b1 = new byte[]{'1'};
        CodecBuffer car = Buffers.wrap(b0, 0, b0.length);
        CodecBuffer cdr = Buffers.newCodecBuffer(0);
        CodecBuffer added = Buffers.wrap(b1, 0, b1.length);

        BufferSinkList sut = new BufferSinkList(car, cdr);
        sut.addFirst(added);

        assertThat(sut.remainingBytes(), is(2));
        assertThat(sut.car().remainingBytes(), is(2));
        byte[] read = new byte[2];
        car.readBytes(read, 0, 2);
        assertThat(read, is(new byte[]{'1', '0'}));
        assertThat(sut.cdr().remainingBytes(), is(0));
    }

    @Test
    public void testAddLast() throws Exception {
        byte[] b0 = new byte[]{'0'};
        byte[] b1 = new byte[]{'1'};
        CodecBuffer car = Buffers.newCodecBuffer(0);
        CodecBuffer cdr = Buffers.wrap(b0, 0, b0.length);
        CodecBuffer added = Buffers.wrap(b1, 0, b1.length);

        BufferSinkList sut = new BufferSinkList(car, cdr);
        sut.addLast(added);

        assertThat(sut.remainingBytes(), is(2));
        assertThat(sut.car().remainingBytes(), is(0));
        assertThat(sut.cdr().remainingBytes(), is(2));
        byte[] read = new byte[2];
        cdr.readBytes(read, 0, 2);
        assertThat(read, is(new byte[]{'0', '1'}));
    }

    @Test
    public void testSlice_ExceedingBytes() throws Exception {
        BufferSink car = Buffers.wrap(new byte[1], 0, 1);
        BufferSink cdr = Buffers.wrap(new byte[2], 0, 2);
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("Invalid input 4. 3 byte remains.");

        BufferSink sut = Buffers.wrap(car, cdr);
        sut.slice(4);
    }

    @Test
    public void testSlice_NegativeInput() throws Exception {
        BufferSink car = Buffers.newCodecBuffer(0);
        BufferSink cdr = Buffers.newCodecBuffer(0);
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("Invalid input -1. 0 byte remains.");

        BufferSink sut = Buffers.wrap(car, cdr);
        sut.slice(-1);
    }

    @Test
    public void testSlice_Empty() throws Exception {
        BufferSink car = Buffers.newCodecBuffer(0);
        BufferSink cdr = Buffers.newCodecBuffer(0);

        BufferSink sut = Buffers.wrap(car, cdr);
        BufferSink sliced = sut.slice(0);

        assertThat(sliced.remainingBytes(), is(0));
    }

    @Test
    public void testSlice_CarOnly() throws Exception {
        BufferSink car = Buffers.wrap(new byte[2], 0, 2);
        BufferSink cdr = Buffers.wrap(new byte[3], 0, 3);

        BufferSink sut = Buffers.wrap(car, cdr);
        BufferSink sliced = sut.slice(2);

        assertThat(sliced.remainingBytes(), is(2));
        assertThat(car.remainingBytes(), is(0));
        assertThat(cdr.remainingBytes(), is(3));
    }

    @Test
    public void testSlice_CarAndCdr() throws Exception {
        BufferSink car = Buffers.wrap(new byte[2], 0, 2);
        BufferSink cdr = Buffers.wrap(new byte[3], 0, 3);

        BufferSink sut = Buffers.wrap(car, cdr);
        BufferSink sliced = sut.slice(3);

        assertThat(sliced.remainingBytes(), is(3));
        assertThat(car.remainingBytes(), is(0));
        assertThat(cdr.remainingBytes(), is(2));
    }

    @Test
    public void testDuplicate_Independent() throws Exception {
        byte[] carData = new byte[2];
        Arrays.fill(carData, (byte) 1);
        BufferSink car = Buffers.wrap(carData, 0, carData.length);
        byte[] cdrData = new byte[3];
        Arrays.fill(cdrData, (byte) 2);
        BufferSink cdr = Buffers.wrap(cdrData, 0, cdrData.length);
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
             @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                 ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
                 int remaining = buffer.remaining();
                 buffer.limit(buffer.capacity());
                 return remaining;
            }
        });

        BufferSinkList sut = new BufferSinkList(car, cdr);
        BufferSinkList duplicated = sut.duplicate();
        sut.transferTo(channel);

        assertThat(duplicated.remainingBytes(), is(2 + 3));
        assertThat(sut.remainingBytes(), is(0));
        assertThat(duplicated.car(), is(not(sameInstance(sut.car()))));
        assertThat(duplicated.cdr(), is(not(sameInstance(sut.cdr()))));
    }
}
