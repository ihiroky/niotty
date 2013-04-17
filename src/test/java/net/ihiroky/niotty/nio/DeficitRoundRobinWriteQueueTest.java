package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class DeficitRoundRobinWriteQueueTest {

    static class FlushAllAnswer implements Answer<Integer> {
        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
            bb.position(bb.limit());
            return bb.limit();
        }
    }

    static class FlushSizeAnswer implements Answer<Integer> {
        final int flushSize_;

        FlushSizeAnswer(int flushSize) {
            flushSize_ = flushSize;
        }

        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
            bb.position(bb.position() + flushSize_);
            return flushSize_;
        }
    }

    @Test
    public void testConstructor() throws Exception {
        DeficitRoundRobinWriteQueue sut = new DeficitRoundRobinWriteQueue(32, 50, 25);
        assertThat(sut.weights(0), is(50));
        assertThat(sut.weights(1), is(25));
        assertThat(sut.deficitCounter(0), is(0));
        assertThat(sut.deficitCounter(1), is(0));
        assertThat(sut.baseQueueLimit(), is(32));
        assertThat(sut.queueIndex(), is(-1));
    }

    @Test
    public void testFlushTo_Base() throws Exception {
        DeficitRoundRobinWriteQueue sut = new DeficitRoundRobinWriteQueue(32);
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new FlushAllAnswer());
        byte[] data = new byte[] {
                '0', '1', '2', '3'
        };

        sut.offer(Buffers.newCodecBuffer(data, 0, data.length));
        WriteQueue.FlushStatus status = sut.flushTo(channel, ByteBuffer.allocate(16));

        assertThat(status, is(WriteQueue.FlushStatus.FLUSHED));
        assertThat(sut.lastFlushedBytes(), is(4));
        assertThat(sut.queueIndex(), is(-1));
    }

    @Test
    public void testFlushTo_BaseIsFlushing() throws Exception {
        DeficitRoundRobinWriteQueue sut = new DeficitRoundRobinWriteQueue(32, 100, 50, 25);
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new FlushSizeAnswer(4));
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7'
        };

        sut.offer(Buffers.newCodecBuffer(data, 0, data.length));
        sut.offer(Buffers.newCodecBuffer(data, 0, data.length, 0));
        sut.offer(Buffers.newCodecBuffer(data, 0, data.length, 2));
        WriteQueue.FlushStatus status = sut.flushTo(channel, ByteBuffer.allocate(16));

        assertThat(status, is(WriteQueue.FlushStatus.FLUSHING));
        assertThat(sut.lastFlushedBytes(), is(4));
        assertThat(sut.queueIndex(), is(-1));
        assertThat(sut.deficitCounter(0), is(4));
        assertThat(sut.deficitCounter(1), is(0)); // not counted up
        assertThat(sut.deficitCounter(2), is(1));
    }

    @Test
    public void testFlushTo_DeficitCounterGetsZeroIfQueueIsEmpty() throws Exception {
        DeficitRoundRobinWriteQueue sut = new DeficitRoundRobinWriteQueue(32, 100, 50);
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new FlushAllAnswer());
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7'
        };
        sut.deficitCounter(0, 100);
        sut.deficitCounter(1, 100);

        sut.offer(Buffers.newCodecBuffer(data, 0, data.length));
        sut.offer(Buffers.newCodecBuffer(data, 0, data.length / 2, 0));
        WriteQueue.FlushStatus status = sut.flushTo(channel, ByteBuffer.allocate(32));

        assertThat(status, is(WriteQueue.FlushStatus.FLUSHED));
        assertThat(sut.lastFlushedBytes(), is(8 + 4));
        assertThat(sut.queueIndex(), is(-1));
        assertThat(sut.deficitCounter(0), is(100 + 8 - 4));
        assertThat(sut.deficitCounter(1), is(0)); // gets zero
    }

    @Test
    public void testFlushTo_DoNotFlushToPartiallyIfDeficitCounterIsShort() throws Exception {
        DeficitRoundRobinWriteQueue sut = new DeficitRoundRobinWriteQueue(32, 100, 50);
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new FlushAllAnswer());
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7'
        };
        sut.deficitCounter(0, 0); // priority 0 may not be executed.
        sut.deficitCounter(1, 6);

        sut.offer(Buffers.newCodecBuffer(data, 0, data.length / 2));
        sut.offer(Buffers.newCodecBuffer(data, 0, data.length, 0));
        sut.offer(Buffers.newCodecBuffer(data, 0, data.length, 1));
        WriteQueue.FlushStatus status = sut.flushTo(channel, ByteBuffer.allocate(32));

        assertThat(status, is(WriteQueue.FlushStatus.SKIP));
        assertThat(sut.lastFlushedBytes(), is(4 + 8));
        assertThat(sut.queueIndex(), is(-1));
        assertThat(sut.deficitCounter(0), is(4)); // queue -1 input * 1f
        assertThat(sut.deficitCounter(1), is(6 + 2 - 8)); // original + (queue -1 input) * 0.5f - (queue 1 input)
    }

    @Test
    public void testFlushTo_PriorityQueueIsFlushing() throws Exception {
        DeficitRoundRobinWriteQueue sut = new DeficitRoundRobinWriteQueue(64, 100, 50);
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class)))
                .thenAnswer(new FlushAllAnswer())
                .thenAnswer(new FlushSizeAnswer(3))
                .thenAnswer(new FlushAllAnswer());
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7'
        };
        sut.deficitCounter(0, 0);
        sut.deficitCounter(1, 0);

        sut.offer(Buffers.newCodecBuffer(data, 0, data.length));
        sut.offer(Buffers.newCodecBuffer(data, 0, data.length, 0));
        sut.offer(Buffers.newCodecBuffer(data, 0, data.length, 1));
        WriteQueue.FlushStatus status = sut.flushTo(channel, ByteBuffer.allocate(32));

        assertThat(status, is(WriteQueue.FlushStatus.FLUSHING));
        assertThat(sut.lastFlushedBytes(), is(8 + 3)); // (queue -1) + (queue 0 (limited))
        assertThat(sut.queueIndex(), is(0));
        assertThat(sut.deficitCounter(0), is(8 - 3)); // (queue -1) - (queue 0 (flush size))
        assertThat(sut.deficitCounter(1), is(4)); // (queue -1) * 0.5f, queue 1 is not executed
    }

    @Test
    public void testFlushTo_UseRoundBonusIfBaseQueueIsEmpty() throws Exception {
        DeficitRoundRobinWriteQueue sut = new DeficitRoundRobinWriteQueue(64, 100, 50);
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new FlushAllAnswer());
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7'
        };
        sut.deficitCounter(0, 0);
        sut.deficitCounter(1, 0);

        sut.offer(Buffers.newCodecBuffer(data, 0, data.length, 0));
        sut.offer(Buffers.newCodecBuffer(data, 0, data.length, 1));
        WriteQueue.FlushStatus status = sut.flushTo(channel, ByteBuffer.allocate(32));

        assertThat(status, is(WriteQueue.FlushStatus.FLUSHED));
        assertThat(sut.lastFlushedBytes(), is(8 + 8));
        assertThat(sut.queueIndex(), is(-1));
        assertThat(sut.deficitCounter(0), is(64 / 2 - 8)); // 64 / 2 * 1.0
        assertThat(sut.deficitCounter(1), is(64 / 4 - 8)); // 64 / 2 * 0.5
    }

    @Test
    public void testFlushTo_RecursiveCallIfQueueIndexIsNotMinus1() throws Exception {
        DeficitRoundRobinWriteQueue sut = new DeficitRoundRobinWriteQueue(64, 100, 50);
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new FlushAllAnswer());
        byte[] data = new byte[] {
                '0', '1', '2', '3', '4', '5', '6', '7'
        };
        // in this situation, deficit counter pointed by queueIndex is large enough to flush.
        sut.queueIndex(1);
        sut.deficitCounter(1, 8);

        sut.offer(Buffers.newCodecBuffer(data, 0, data.length));
        sut.offer(Buffers.newCodecBuffer(data, 0, data.length - 1, 0)); // only 7 byte
        sut.offer(Buffers.newCodecBuffer(data, 0, data.length, 1)); // flush first
        WriteQueue.FlushStatus status = sut.flushTo(channel, ByteBuffer.allocate(32));

        assertThat(status, is(WriteQueue.FlushStatus.FLUSHED));
        assertThat(sut.lastFlushedBytes(), is(8 + 7 + 8));
        assertThat(sut.queueIndex(), is(-1));
        assertThat(sut.deficitCounter(0), is(8 - 7));
        assertThat(sut.deficitCounter(1), is(0)); // (queue 1) is empty at last.
    }

    @Test
    public void testClear() throws Exception {
        BufferSink bufferSink0 = mock(BufferSink.class);
        doNothing().when(bufferSink0).dispose();
        doReturn(-1).when(bufferSink0).priority();
        BufferSink bufferSink1 = mock(BufferSink.class);
        doNothing().when(bufferSink1).dispose();
        doReturn(0).when(bufferSink1).priority();
        BufferSink bufferSink2 = mock(BufferSink.class);
        doNothing().when(bufferSink2).dispose();
        doReturn(1).when(bufferSink2).priority();

        DeficitRoundRobinWriteQueue sut = new DeficitRoundRobinWriteQueue(64, 100, 100);
        sut.offer(bufferSink0);
        sut.offer(bufferSink1);
        sut.offer(bufferSink2);
        sut.clear();

        verify(bufferSink0, times(1)).dispose();
        verify(bufferSink1, times(1)).dispose();
        verify(bufferSink2, times(1)).dispose();
    }
}
