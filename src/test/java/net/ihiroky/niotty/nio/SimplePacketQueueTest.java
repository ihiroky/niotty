package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.GatheringByteChannel;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class SimplePacketQueueTest {

    private SimplePacketQueue sut_;

    @Before
    public void setUp() throws Exception {
        sut_ = new SimplePacketQueue();
    }

    @Test
    public void testSize() throws Exception {
        sut_.offer(Buffers.emptyBuffer());
        sut_.offer(Buffers.emptyBuffer());
        sut_.offer(Buffers.emptyBuffer());

        assertThat(sut_.size(), is(3));
    }

    @Test
    public void testIsEmptyReturnsTrueIfNoElementIsFound() throws Exception {
        assertThat(sut_.isEmpty(), is(true));
    }

    @Test
    public void testIsEmptyReturnsFalseIfAnyElementIsFound() throws Exception {
        sut_.offer(Buffers.emptyBuffer());
        assertThat(sut_.isEmpty(), is(false));
    }

    @Test
    public void testClear() throws Exception {
        sut_.offer(Buffers.emptyBuffer());
        sut_.clear();

        assertThat(sut_.isEmpty(), is(true));
    }

    @Test
    public void testFlushAllElements() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        BufferSink p0 = mock(BufferSink.class);
        BufferSink p1 = mock(BufferSink.class);
        BufferSink p2 = mock(BufferSink.class);
        when(p0.sink(channel)).thenReturn(true);
        when(p1.sink(channel)).thenReturn(true);
        when(p2.sink(channel)).thenReturn(true);
        sut_.offer(p0);
        sut_.offer(p1);
        sut_.offer(p2);

        FlushStatus status = sut_.flush(channel);

        assertThat(status, is(FlushStatus.FLUSHED));
        verify(p0).sink(channel);
        verify(p1).sink(channel);
        verify(p2).sink(channel);
    }

    @Test
    public void testFlushPartOfElements() throws Exception {
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        BufferSink p0 = mock(BufferSink.class);
        BufferSink p1 = mock(BufferSink.class);
        BufferSink p2 = mock(BufferSink.class);
        when(p0.sink(channel)).thenReturn(true);
        when(p1.sink(channel)).thenReturn(false);
        when(p2.sink(channel)).thenReturn(true);
        sut_.offer(p0);
        sut_.offer(p1);
        sut_.offer(p2);

        FlushStatus status = sut_.flush(channel);

        assertThat(status, is(FlushStatus.FLUSHING));
        verify(p0).sink(channel);
        verify(p1).sink(channel);
        verify(p2, never()).sink(channel);
    }
}
