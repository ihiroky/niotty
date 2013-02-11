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
 * Created on 13/02/08, 17:07
 *
 * @author Hiroki Itoh
 */
public class ArrayBufferSinkTest {

    private ArrayBufferSink sut;
    private int dataLength;

    @Before
    public void setUp() {
        byte[] data = new byte[32];
        Arrays.fill(data, (byte) '0');
        sut = new ArrayBufferSink(data, 0, data.length);
        dataLength = data.length;
    }

    @Test
    public void testTransferToWriteOnce() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(dataLength);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(buffer)).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ByteBuffer bb = (ByteBuffer) args[0];
                bb.position(bb.limit());
                return bb.limit();
            }
        });

        assertThat(sut.remainingBytes(), is(dataLength));

        boolean result = sut.transferTo(channel, buffer);

        assertThat(result, is(true));
        assertThat(sut.remainingBytes(), is(0));
        verify(channel, times(1)).write(buffer);
    }

    @Test
    public void testTransferToWriteThreeTimes() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocateDirect(dataLength / 3 + 1);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(buffer)).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ByteBuffer bb = (ByteBuffer) args[0];
                bb.position(bb.limit());
                return bb.limit();
            }
        });

        assertThat(sut.remainingBytes(), is(dataLength));

        boolean result = sut.transferTo(channel, buffer);

        assertThat(result, is(true));
        assertThat(sut.remainingBytes(), is(0));
        verify(channel, times(3)).write(buffer);
    }

    @Test
    public void testTransferToNotEnoughWrite() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocateDirect(dataLength);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(buffer)).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ByteBuffer bb = (ByteBuffer) args[0];
                bb.position(bb.limit() - 1);
                return dataLength - 1;
            }
        });

        assertThat(sut.remainingBytes(), is(dataLength));

        boolean result = sut.transferTo(channel, buffer);

        assertThat(result, is(false));
        assertThat(sut.remainingBytes(), is(1));
        verify(channel, times(1)).write(buffer);
    }

    @Test(expected = EOFException.class)
    public void testTransferToEOF() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(dataLength);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(buffer)).thenReturn(-1);

        sut.transferTo(channel, buffer);
        verify(channel, times(1)).write(buffer);
    }

    @Test(expected = IOException.class)
    public void testTransferToIOException() throws  Exception {
        ByteBuffer buffer = ByteBuffer.allocate(dataLength);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(buffer)).thenThrow(new IOException());

        sut.transferTo(channel, buffer);
        verify(channel, times(1)).write(buffer);
    }
}
