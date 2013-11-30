package net.ihiroky.niotty.buffer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.GatheringByteChannel;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class PacketListTest {

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Test
    public void testDispose() throws Exception {
        Packet car = mock(Packet.class);
        doNothing().when(car).dispose();
        Packet cdr = mock(Packet.class);
        doNothing().when(cdr).dispose();

        PacketList list = new PacketList(car, cdr);
        list.dispose();

        verify(car, times(1)).dispose();
        verify(cdr, times(1)).dispose();
    }

    @Test
    public void testSink_CarAndCdrIsTrue() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        Packet car = mock(Packet.class);
        doReturn(true).when(car).sink(channel);
        Packet cdr = mock(Packet.class);
        doReturn(true).when(cdr).sink(channel);

        PacketList List = new PacketList(car, cdr);
        boolean result = List.sink(channel);

        assertThat(result, is(true));
        verify(car, times(1)).sink(channel);
        verify(cdr, times(1)).sink(channel);
    }

    @Test
    public void testSink_CarIsTrueAndCdrIsFalse() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        Packet car = mock(Packet.class);
        doReturn(true).when(car).sink(channel);
        Packet cdr = mock(Packet.class);
        doReturn(false).when(cdr).sink(channel);

        PacketList List = new PacketList(car, cdr);
        boolean result = List.sink(channel);

        assertThat(result, is(false));
        verify(car, times(1)).sink(channel);
        verify(cdr, times(1)).sink(channel);
    }

    @Test
    public void testSink_CarAndCdrIsFalse() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        Packet car = mock(Packet.class);
        doReturn(false).when(car).sink(channel);
        Packet cdr = mock(Packet.class);
        doReturn(false).when(cdr).sink(channel);

        PacketList sut = new PacketList(car, cdr);
        boolean result = sut.sink(channel);

        assertThat(result, is(false));
        verify(car, times(1)).sink(channel);
        verify(cdr, never()).sink(channel);
    }

    @Test
    public void testSinkDatagram_FlushAll() throws Exception {
        byte[] carData = new byte[]{0};
        Packet car = Buffers.wrap(carData, 0, carData.length);
        byte[] cdrData = new byte[]{1, 2};
        Packet cdr = Buffers.wrap(cdrData, 0, cdrData.length);
        DatagramChannel channel = mock(DatagramChannel.class);
        ByteBuffer buffer = ByteBuffer.allocate(3);
        SocketAddress target = new InetSocketAddress(12345);
        when(channel.send(buffer, target)).thenReturn(3);
        PacketList sut = new PacketList(car, cdr);

        boolean result = sut.sink(channel, buffer, target);

        assertThat(result, is(true));
        assertThat(buffer.remaining(), is(buffer.capacity()));
        verify(channel).send(ByteBuffer.wrap(new byte[]{0, 1, 2}), target);
    }

    @Test
    public void testSinDatagram_FlushNothing() throws Exception {
        byte[] carData = new byte[]{0};
        Packet car = Buffers.wrap(carData, 0, carData.length);
        byte[] cdrData = new byte[]{1, 2};
        Packet cdr = Buffers.wrap(cdrData, 0, cdrData.length);
        DatagramChannel channel = mock(DatagramChannel.class);
        ByteBuffer buffer = ByteBuffer.allocate(3);
        SocketAddress target = new InetSocketAddress(12345);
        when(channel.send(buffer, target)).thenReturn(0);
        PacketList sut = new PacketList(car, cdr);

        boolean result = sut.sink(channel, buffer, target);

        assertThat(result, is(false));
        assertThat(buffer.remaining(), is(buffer.capacity()));
        verify(channel).send(ByteBuffer.wrap(new byte[]{0, 1, 2}), target);
    }

    @Test
    public void testCopyTo_WriteAllDataIfByteBufferIsLargeEnough() throws Exception {
        byte[] carData = new byte[]{0};
        Packet car = Buffers.wrap(carData, 0, carData.length);
        byte[] cdrData = new byte[]{1, 2};
        Packet cdr = Buffers.wrap(cdrData, 0, cdrData.length);
        ByteBuffer buffer = ByteBuffer.allocate(3);

        PacketList sut = new PacketList(car, cdr);
        sut.copyTo(buffer);
        buffer.flip();

        assertThat(buffer, is(ByteBuffer.wrap(new byte[]{0, 1, 2})));
        assertThat(car.remaining(), is(1));
        assertThat(cdr.remaining(), is(2));
    }

    @Test
    public void testAddFirst() throws Exception {
        byte[] b0 = new byte[]{'0'};
        byte[] b1 = new byte[]{'1'};
        CodecBuffer car = Buffers.wrap(b0, 0, b0.length);
        CodecBuffer cdr = Buffers.newCodecBuffer(0);
        CodecBuffer added = Buffers.wrap(b1, 0, b1.length);

        PacketList sut = new PacketList(car, cdr);
        sut.addFirst(added);

        assertThat(sut.remaining(), is(2));
        assertThat(sut.car().remaining(), is(2));
        byte[] read = new byte[2];
        car.readBytes(read, 0, 2);
        assertThat(read, is(new byte[]{'1', '0'}));
        assertThat(sut.cdr().remaining(), is(0));
    }

    @Test
    public void testAddLast() throws Exception {
        byte[] b0 = new byte[]{'0'};
        byte[] b1 = new byte[]{'1'};
        CodecBuffer car = Buffers.newCodecBuffer(0);
        CodecBuffer cdr = Buffers.wrap(b0, 0, b0.length);
        CodecBuffer added = Buffers.wrap(b1, 0, b1.length);

        PacketList sut = new PacketList(car, cdr);
        sut.addLast(added);

        assertThat(sut.remaining(), is(2));
        assertThat(sut.car().remaining(), is(0));
        assertThat(sut.cdr().remaining(), is(2));
        byte[] read = new byte[2];
        cdr.readBytes(read, 0, 2);
        assertThat(read, is(new byte[]{'0', '1'}));
    }

    @Test
    public void testSlice_ExceedingBytes() throws Exception {
        Packet car = Buffers.wrap(new byte[1], 0, 1);
        Packet cdr = Buffers.wrap(new byte[2], 0, 2);
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("Invalid input 4. 3 byte remains.");

        Packet sut = Buffers.wrap(car, cdr);
        sut.slice(4);
    }

    @Test
    public void testSlice_NegativeInput() throws Exception {
        Packet car = Buffers.newCodecBuffer(0);
        Packet cdr = Buffers.newCodecBuffer(0);
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("Invalid input -1. 0 byte remains.");

        Packet sut = Buffers.wrap(car, cdr);
        sut.slice(-1);
    }

    @Test
    public void testSlice_Empty() throws Exception {
        Packet car = Buffers.newCodecBuffer(0);
        Packet cdr = Buffers.newCodecBuffer(0);

        Packet sut = Buffers.wrap(car, cdr);
        Packet sliced = sut.slice(0);

        assertThat(sliced.remaining(), is(0));
    }

    @Test
    public void testSlice_CarOnly() throws Exception {
        Packet car = Buffers.wrap(new byte[2], 0, 2);
        Packet cdr = Buffers.wrap(new byte[3], 0, 3);

        Packet sut = Buffers.wrap(car, cdr);
        Packet sliced = sut.slice(2);

        assertThat(sliced.remaining(), is(2));
        assertThat(car.remaining(), is(0));
        assertThat(cdr.remaining(), is(3));
    }

    @Test
    public void testSlice_CarAndCdr() throws Exception {
        Packet car = Buffers.wrap(new byte[2], 0, 2);
        Packet cdr = Buffers.wrap(new byte[3], 0, 3);

        Packet sut = Buffers.wrap(car, cdr);
        Packet sliced = sut.slice(3);

        assertThat(sliced.remaining(), is(3));
        assertThat(car.remaining(), is(0));
        assertThat(cdr.remaining(), is(2));
    }

    @Test
    public void testDuplicate_Independent() throws Exception {
        byte[] carData = new byte[2];
        Arrays.fill(carData, (byte) 1);
        Packet car = Buffers.wrap(carData, 0, carData.length);
        byte[] cdrData = new byte[3];
        Arrays.fill(cdrData, (byte) 2);
        Packet cdr = Buffers.wrap(cdrData, 0, cdrData.length);
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

        PacketList sut = new PacketList(car, cdr);
        PacketList duplicated = sut.duplicate();
        sut.sink(channel);

        assertThat(duplicated.remaining(), is(2 + 3));
        assertThat(sut.remaining(), is(0));
        assertThat(duplicated.car(), is(not(sameInstance(sut.car()))));
        assertThat(duplicated.cdr(), is(not(sameInstance(sut.cdr()))));
    }

    @Test
    public void testEquals() throws Exception {
        byte[] data0 = new byte[2];
        Arrays.fill(data0, (byte) 1);
        Packet bs0 = Buffers.wrap(data0, 0, data0.length);
        byte[] data1 = new byte[3];
        Arrays.fill(data1, (byte) 1);
        Packet bs1 = Buffers.wrap(data1, 0, data1.length);

        PacketList sut = new PacketList(bs0, bs1);
        PacketList obj = new PacketList(bs1, bs0);
        assertThat(sut.equals(obj), is(true));
    }
}
