package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import org.junit.Before;
import org.junit.Test;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class SimpleDatagramQueueTest {

    private SimpleDatagramQueue sut_;

    @Before
    public void setUp() throws Exception {
        sut_ = new SimpleDatagramQueue();
    }

    @Test
    public void testSize() throws Exception {
        sut_.offer(new AttachedMessage<BufferSink>(Buffers.emptyBuffer(), new Object()));
        sut_.offer(new AttachedMessage<BufferSink>(Buffers.emptyBuffer(), new Object()));
        sut_.offer(new AttachedMessage<BufferSink>(Buffers.emptyBuffer(), new Object()));

        assertThat(sut_.size(), is(3));
    }

    @Test
    public void testIsEmptyReturnsTrueIfNoElementIsFound() throws Exception {
        assertThat(sut_.isEmpty(), is(true));
    }

    @Test
    public void testIsEmptyReturnsFalseIfAnyElementIsFound() throws Exception {
        sut_.offer(new AttachedMessage<BufferSink>(Buffers.emptyBuffer(), new Object()));
        assertThat(sut_.isEmpty(), is(false));
    }

    @Test
    public void testClear() throws Exception {
        sut_.offer(new AttachedMessage<BufferSink>(Buffers.emptyBuffer(), new Object()));
        sut_.clear();

        assertThat(sut_.isEmpty(), is(true));
    }

    @Test
    public void testFlushAllElements() throws Exception {
        DatagramChannel channel = mock(DatagramChannel.class);
        SocketAddress target = mock(SocketAddress.class);
        ByteBuffer buffer = ByteBuffer.allocate(0);
        BufferSink p0 = mock(BufferSink.class);
        BufferSink p1 = mock(BufferSink.class);
        BufferSink p2 = mock(BufferSink.class);
        when(p0.sink(channel, buffer, target)).thenReturn(true);
        when(p1.sink(channel, buffer, target)).thenReturn(true);
        when(p2.sink(channel, buffer, target)).thenReturn(true);
        sut_.offer(new AttachedMessage<BufferSink>(p0, target));
        sut_.offer(new AttachedMessage<BufferSink>(p1, target));
        sut_.offer(new AttachedMessage<BufferSink>(p2, target));

        FlushStatus status = sut_.flush(channel, buffer);

        assertThat(status, is(FlushStatus.FLUSHED));
        verify(p0).sink(channel, buffer, target);
        verify(p1).sink(channel, buffer, target);
        verify(p2).sink(channel, buffer, target);
    }

    @Test
    public void testFlushPartOfElements() throws Exception {
        DatagramChannel channel = mock(DatagramChannel.class);
        SocketAddress target = mock(SocketAddress.class);
        ByteBuffer buffer = ByteBuffer.allocate(0);
        BufferSink p0 = mock(BufferSink.class);
        BufferSink p1 = mock(BufferSink.class);
        BufferSink p2 = mock(BufferSink.class);
        when(p0.sink(channel, buffer, target)).thenReturn(true);
        when(p1.sink(channel, buffer, target)).thenReturn(false);
        when(p2.sink(channel, buffer, target)).thenReturn(true);
        sut_.offer(new AttachedMessage<BufferSink>(p0, target));
        sut_.offer(new AttachedMessage<BufferSink>(p1, target));
        sut_.offer(new AttachedMessage<BufferSink>(p2, target));

        FlushStatus status = sut_.flush(channel, buffer);

        assertThat(status, is(FlushStatus.FLUSHING));
        verify(p0).sink(channel, buffer, target);
        verify(p1).sink(channel, buffer, target);
        verify(p2, never()).sink(channel);
    }
}
