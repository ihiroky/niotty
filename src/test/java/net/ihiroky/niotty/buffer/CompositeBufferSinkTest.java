package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class CompositeBufferSinkTest {

    private CompositeBufferSink sut_;
    private int dataLength_;

    @Before
    public void setUp() {
        dataLength_ = 32;
        byte[] data = new byte[dataLength_ / 2];
        Arrays.fill(data, (byte) '0');
        sut_ = new CompositeBufferSink(Arrays.asList(
                Buffers.newEncodeBuffer(data, 0, data.length), Buffers.newEncodeBuffer(data, 0, data.length)));
    }

    @Test
    public void testTransferToOnce() throws Exception {
        ByteBuffer writeBuffer = ByteBuffer.allocate(dataLength_);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(writeBuffer)).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ByteBuffer bb = (ByteBuffer) args[0];
                bb.position(bb.limit());
                return bb.limit();
            }
        });

        assertThat(sut_.remainingBytes(), is(dataLength_));

        boolean result = sut_.transferTo(channel, writeBuffer);

        assertThat(result, is(true));
        assertThat(sut_.remainingBytes(), is(0));
        verify(channel, times(2)).write(writeBuffer);
    }

    @Test
    public void testTransferToWriteFourTimes() throws Exception {
        ByteBuffer writeBuffer = ByteBuffer.allocate(dataLength_ / 4);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(writeBuffer)).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable { // read first BufferSink whole.
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(dataLength_ / 4);
                return dataLength_ / 4;
            }
        });

        assertThat(sut_.remainingBytes(), is(dataLength_));

        boolean result = sut_.transferTo(channel, writeBuffer);

        assertThat(result, is(true));
        assertThat(sut_.remainingBytes(), is(0));
        verify(channel, times(4)).write(writeBuffer);
    }

    @Test
    public void testTransferToNotEnoughWrite() throws Exception {
        ByteBuffer writeBuffer = ByteBuffer.allocate(dataLength_);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(writeBuffer)).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable { // read first BufferSink
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(dataLength_ / 2);
                return dataLength_ / 2;
            }
        }).thenAnswer(new Answer<Object>() { // read second BufferSink, but 1 byte left in writeBuffer
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(dataLength_ / 2 - 1);
                return dataLength_ / 2 - 1;
            }
        });

        assertThat(sut_.remainingBytes(), is(dataLength_));

        boolean result = sut_.transferTo(channel, writeBuffer);

        assertThat(result, is(false));
        assertThat(sut_.remainingBytes(), is(0)); // last one byte is put in writeBuffer;
        assertThat(writeBuffer.remaining(), is(1));
        verify(channel, times(2)).write(writeBuffer);
    }

    @Test(expected = EOFException.class)
    public void testTransferToEOF() throws Exception {
        ByteBuffer writeBuffer = ByteBuffer.allocate(dataLength_);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(writeBuffer)).thenReturn(-1);

        sut_.transferTo(channel, writeBuffer);
    }

    @Test(expected = IOException.class)
    public void testTransferToIOException() throws  Exception {
        ByteBuffer writeBuffer = ByteBuffer.allocate(dataLength_);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(writeBuffer)).thenThrow(new IOException());

        sut_.transferTo(channel, writeBuffer);
    }
}
