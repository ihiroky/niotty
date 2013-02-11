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
public class ByteBufferBufferSinkTest {

    private ByteBufferBufferSink sut;
    private int dataLength;

    @Before
    public void setUp() {
        dataLength = 32;
        byte[] data = new byte[dataLength];
        Arrays.fill(data, (byte) '0');
        sut = new ByteBufferBufferSink(ByteBuffer.wrap(data));
    }

    @Test
    public void testTransferToOnce() throws Exception {
        ByteBuffer writeBuffer = ByteBuffer.allocate(dataLength);
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

        assertThat(sut.remainingBytes(), is(dataLength));

        boolean result = sut.transferTo(channel, writeBuffer);

        assertThat(result, is(true));
        assertThat(sut.remainingBytes(), is(0));
        verify(channel, times(1)).write(writeBuffer);
    }

    @Test
    public void testTransferToWriteThreeTimes() throws Exception {
        ByteBuffer writeBuffer = ByteBuffer.allocate(dataLength / 3 + 1);
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

        assertThat(sut.remainingBytes(), is(dataLength));

        boolean result = sut.transferTo(channel, writeBuffer);

        assertThat(result, is(true));
        assertThat(sut.remainingBytes(), is(0));
        verify(channel, times(3)).write(writeBuffer);
    }

    @Test
    public void testTransferToNotEnoughWrite() throws Exception {
        ByteBuffer writeBuffer = ByteBuffer.allocate(dataLength);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(writeBuffer)).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ByteBuffer bb = (ByteBuffer) args[0];
                bb.position(dataLength - 1);
                return dataLength - 1;
            }
        });

        assertThat(sut.remainingBytes(), is(dataLength));

        boolean result = sut.transferTo(channel, writeBuffer);

        assertThat(result, is(false));
        assertThat(sut.remainingBytes(), is(0)); // last one byte is put in writeBuffer;
        assertThat(writeBuffer.remaining(), is(1));
        verify(channel, times(1)).write(writeBuffer);
    }

    @Test(expected = EOFException.class)
    public void testTransferToEOF() throws Exception {
        ByteBuffer writeBuffer = ByteBuffer.allocate(dataLength);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(writeBuffer)).thenReturn(-1);

        sut.transferTo(channel, writeBuffer);
    }

    @Test(expected = IOException.class)
    public void testTransferToIOException() throws  Exception {
        ByteBuffer writeBuffer = ByteBuffer.allocate(dataLength);
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(writeBuffer)).thenThrow(new IOException());

        sut.transferTo(channel, writeBuffer);
    }
}
