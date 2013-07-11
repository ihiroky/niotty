package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class SimpleWriteQueueTest {

    private SimpleWriteQueue sut_;
    private ByteBuffer writeBuffer_;

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Before
    public void setUp() {
        sut_ = new SimpleWriteQueue();
        writeBuffer_ = ByteBuffer.allocate(16);
    }

    @Test
    public void testFlushTo_Empty() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        WriteQueue.FlushStatus status = sut_.flushTo(channel);
        assertThat(status, is(WriteQueue.FlushStatus.FLUSHED));
        verify(channel, never()).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushTo_DatagramChannel_Empty() throws Exception {
        DatagramChannel channel = mock(DatagramChannel.class);
        WriteQueue.FlushStatus status = sut_.flushTo(channel, writeBuffer_);
        assertThat(status, is(WriteQueue.FlushStatus.FLUSHED));
        verify(channel, never()).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushToOnce() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
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

        sut_.offer(new AttachedMessage<BufferSink>(Buffers.wrap(data, 0, data.length)));
        WriteQueue.FlushStatus status = sut_.flushTo(channel);

        assertThat(status, is(WriteQueue.FlushStatus.FLUSHED));
        assertThat(sut_.lastFlushedBytes(), is(data.length));
        verify(channel, times(1)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushTo_Datagram_Once() throws Exception {
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };
        SocketAddress target =  new InetSocketAddress(0);
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);
        DatagramChannel channel = mock(DatagramChannel.class);
        when(channel.send(writeBuffer_, target)).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(bb.limit());
                return bb.limit();
            }
        });

        sut_.offer(new AttachedMessage<BufferSink>(buffer, new DefaultTransportParameter(target)));
        WriteQueue.FlushStatus status = sut_.flushTo(channel, writeBuffer_);

        assertThat(status, is(WriteQueue.FlushStatus.FLUSHED));
        assertThat(sut_.lastFlushedBytes(), is(data.length));
        verify(channel, times(1)).send(writeBuffer_, target);
    }

    @Test
    public void testFlushToOnceLimitedLastElement() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
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

        sut_.offer(new AttachedMessage<BufferSink>(Buffers.wrap(data, 0, data.length)));
        WriteQueue.FlushStatus status = sut_.flushTo(channel, data.length);

        assertThat(status, is(WriteQueue.FlushStatus.FLUSHED));
        assertThat(sut_.lastFlushedBytes(), is(data.length));
        verify(channel, times(1)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushTo_DatagramChannel_OnceLimitedLastElement() throws Exception {
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);
        DatagramChannel channel = mock(DatagramChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(bb.limit());
                return bb.limit();
            }
        });


        sut_.offer(new AttachedMessage<BufferSink>(buffer));
        WriteQueue.FlushStatus status = sut_.flushTo(channel, writeBuffer_, data.length);

        assertThat(status, is(WriteQueue.FlushStatus.FLUSHED));
        assertThat(sut_.lastFlushedBytes(), is(data.length));
        verify(channel, times(1)).write(writeBuffer_);
    }

    @Test
    public void testFlushToOnceLimitedNotLastElement() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
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

        sut_.offer(new AttachedMessage<BufferSink>(Buffers.wrap(data, 0, data.length)));
        sut_.offer(new AttachedMessage<BufferSink>(Buffers.wrap(data, 0, data.length)));
        WriteQueue.FlushStatus status = sut_.flushTo(channel, data.length);

        assertThat(status, is(WriteQueue.FlushStatus.SKIP));
        assertThat(sut_.lastFlushedBytes(), is(data.length));
        verify(channel, times(1)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushTo_Datagram_OnceLimitedNotLastElement() throws Exception {
        DatagramChannel channel = mock(DatagramChannel.class);
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

        sut_.offer(new AttachedMessage<BufferSink>(Buffers.wrap(data, 0, data.length)));
        sut_.offer(new AttachedMessage<BufferSink>(Buffers.wrap(data, 0, data.length)));
        WriteQueue.FlushStatus status = sut_.flushTo(channel, writeBuffer_, data.length);

        assertThat(status, is(WriteQueue.FlushStatus.SKIP));
        assertThat(sut_.lastFlushedBytes(), is(data.length));
        verify(channel, times(1)).write(writeBuffer_);
    }

    @Test
    public void testFlushToOnceRemaining() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
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

        sut_.offer(new AttachedMessage<BufferSink>(Buffers.wrap(data, 0, data.length)));
        WriteQueue.FlushStatus status = sut_.flushTo(channel);

        assertThat(status, CoreMatchers.is(WriteQueue.FlushStatus.FLUSHING));
        assertThat(sut_.lastFlushedBytes(), is(8));
        verify(channel, times(1)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushToOnceRemainingAndRetryFlushed() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
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
        sut_.offer(new AttachedMessage<BufferSink>(Buffers.wrap(data, 0, data.length)));
        WriteQueue.FlushStatus status = sut_.flushTo(channel);
        assertThat(status, is(WriteQueue.FlushStatus.FLUSHING));
        assertThat(sut_.lastFlushedBytes(), is(8));

        status = sut_.flushTo(channel);

        assertThat(status, CoreMatchers.is(WriteQueue.FlushStatus.FLUSHED));
        assertThat(sut_.lastFlushedBytes(), is(2));
        verify(channel, times(2)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushToOnceRemainingAndRetryFlushing() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
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
        sut_.offer(new AttachedMessage<BufferSink>(Buffers.wrap(data, 0, data.length)));
        WriteQueue.FlushStatus status = sut_.flushTo(channel);
        assertThat(status, is(WriteQueue.FlushStatus.FLUSHING));
        assertThat(sut_.lastFlushedBytes(), is(8));

        status = sut_.flushTo(channel);

        assertThat(status, CoreMatchers.is(WriteQueue.FlushStatus.FLUSHING));
        assertThat(sut_.lastFlushedBytes(), is(0));
        verify(channel, times(2)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlushTo_Datagram_OnceNotWritten() throws Exception {
        DatagramChannel channel = mock(DatagramChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return 0;
            }
        });
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };

        sut_.offer(new AttachedMessage<BufferSink>(Buffers.wrap(data, 0, data.length)));
        WriteQueue.FlushStatus status = sut_.flushTo(channel, writeBuffer_);

        assertThat(status, CoreMatchers.is(WriteQueue.FlushStatus.SKIP));
        assertThat(sut_.lastFlushedBytes(), is(0));
        verify(channel, times(1)).write(writeBuffer_);
    }

    @Test
    public void testClear() throws Exception {
        BufferSink bufferSink = mock(BufferSink.class);
        doNothing().when(bufferSink).dispose();
        sut_.offer(new AttachedMessage<BufferSink>(bufferSink));

        sut_.clear();

        verify(bufferSink, times(1)).dispose();
    }
}
