package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.Buffers;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class SimpleWriteQueueTest {

    private SimpleWriteQueue sut_;

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Before
    public void setUp() {
        sut_ = new SimpleWriteQueue(16, false);
    }

    @Test
    public void testFlushToEmpty() throws Exception {
        WritableByteChannel channel = mock(WritableByteChannel.class);
        NioWriteQueue.FlushStatus status = sut_.flushTo(channel);
        assertThat(status, is(NioWriteQueue.FlushStatus.FLUSHED));
        verify(channel, never()).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushToOnce() throws Exception {
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(bb.limit());
                return bb.limit();
            }
        });
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };

        sut_.offer(Buffers.newCodecBuffer(data, 0, data.length));
        NioWriteQueue.FlushStatus status = sut_.flushTo(channel);

        assertThat(status, is(NioWriteQueue.FlushStatus.FLUSHED));
        assertThat(sut_.lastFlushedBytes(), is(data.length));
        verify(channel, times(1)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushToOnceLazyLimitedLastElement() throws Exception {
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(bb.limit());
                return bb.limit();
            }
        });
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };

        sut_.offer(Buffers.newCodecBuffer(data, 0, data.length));
        NioWriteQueue.FlushStatus status = sut_.flushTo(channel, data.length);

        assertThat(status, is(NioWriteQueue.FlushStatus.FLUSHED));
        assertThat(sut_.lastFlushedBytes(), is(data.length));
        verify(channel, times(1)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushToOnceLazyLimitedNotLastElement() throws Exception {
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(bb.limit());
                return bb.limit();
            }
        });
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };

        sut_.offer(Buffers.newCodecBuffer(data, 0, data.length));
        sut_.offer(Buffers.newCodecBuffer(data, 0, data.length));
        NioWriteQueue.FlushStatus status = sut_.flushTo(channel, data.length);

        assertThat(status, is(NioWriteQueue.FlushStatus.FLUSHING));
        assertThat(sut_.lastFlushedBytes(), is(data.length));
        verify(channel, times(1)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushToOnceRemaining() throws Exception {
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(8);
                return 8;
            }
        });
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };

        sut_.offer(Buffers.newCodecBuffer(data, 0, data.length));
        NioWriteQueue.FlushStatus status = sut_.flushTo(channel);

        assertThat(status, CoreMatchers.is(NioWriteQueue.FlushStatus.FLUSHING));
        assertThat(sut_.lastFlushedBytes(), is(8));
        verify(channel, times(1)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushToOnceRemainingAndRetryFlushed() throws Exception {
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(8);
                return 8;
            }
        }).then(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                // read all remaining
                int remaining = bb.remaining();
                bb.position(bb.limit());
                return remaining;
            }
        });
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };
        sut_.offer(Buffers.newCodecBuffer(data, 0, data.length));
        NioWriteQueue.FlushStatus status = sut_.flushTo(channel);
        assertThat(status, is(NioWriteQueue.FlushStatus.FLUSHING));
        assertThat(sut_.lastFlushedBytes(), is(8));

        status = sut_.flushTo(channel);

        assertThat(status, CoreMatchers.is(NioWriteQueue.FlushStatus.FLUSHED));
        assertThat(sut_.lastFlushedBytes(), is(2));
        verify(channel, times(2)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushToOnceRemainingAndRetryUnexpectedEOF() throws Exception {
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(8);
                return 8;
            }
        }).then(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return -1;
            }
        });
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };
        sut_.offer(Buffers.newCodecBuffer(data, 0, data.length));
        NioWriteQueue.FlushStatus status = sut_.flushTo(channel);
        assertThat(status, is(NioWriteQueue.FlushStatus.FLUSHING));
        assertThat(sut_.lastFlushedBytes(), is(8));

        exceptionRule_.expect(IOException.class);
        exceptionRule_.expectMessage("end of stream.");
        sut_.flushTo(channel);
    }

    public void testFlushToOnceRemainingAndRetryFlushing() throws Exception {
        WritableByteChannel channel = mock(WritableByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(8);
                return 8;
            }
        }).then(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return 0; // read nothing
            }
        });
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };
        sut_.offer(Buffers.newCodecBuffer(data, 0, data.length));
        NioWriteQueue.FlushStatus status = sut_.flushTo(channel);
        assertThat(status, is(NioWriteQueue.FlushStatus.FLUSHING));
        assertThat(sut_.lastFlushedBytes(), is(8));

        status = sut_.flushTo(channel);

        assertThat(status, CoreMatchers.is(NioWriteQueue.FlushStatus.FLUSHING));
        assertThat(sut_.lastFlushedBytes(), is(0));
        verify(channel, times(2)).write(Mockito.any(ByteBuffer.class));
    }

}
