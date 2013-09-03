package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.Task;
import net.ihiroky.niotty.buffer.ArrayCodecBuffer;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.ByteBufferCodecBuffer;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.codec.StageContextMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectionKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class TcpIOSelectorTest {

    private TcpIOSelector sut_;
    private ArgumentCaptor<TcpIOSelector.FlushTask> flushTaskCaptor_;

    @Before
    public void setUp() throws Exception {
        sut_ = spy(new TcpIOSelector(256, false, false));
        flushTaskCaptor_ = ArgumentCaptor.forClass(TcpIOSelector.FlushTask.class);
        doNothing().when(sut_).execute(flushTaskCaptor_.capture());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStore_Flushed() throws Exception {
        NioClientSocketTransport transport = mock(NioClientSocketTransport.class);
        doReturn(WriteQueue.FlushStatus.FLUSHED).when(transport).flush();

        sut_.store(new StageContextMock<Void>(transport, null), Buffers.newCodecBuffer(0));
        TcpIOSelector.FlushTask task = flushTaskCaptor_.getValue();
        task.execute(TimeUnit.MILLISECONDS);

        assertThat(task.flushStatus_.waitTimeMillis_, is(Task.DONE));
        verify(transport).readyToWrite(Mockito.any(AttachedMessage.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStore_FollowingFlushReturnsFlushedOnceFlushingForTheSameTransport() throws Exception {
        NioClientSocketTransport transport = mock(NioClientSocketTransport.class);
        doReturn(WriteQueue.FlushStatus.FLUSHING).when(transport).flush();

        doReturn(null).when(transport).flushStatus();
        sut_.store(new StageContextMock<Void>(transport, null), Buffers.newCodecBuffer(0));
        TcpIOSelector.FlushTask task0 = flushTaskCaptor_.getValue();
        long firstWait = task0.execute(TimeUnit.MILLISECONDS);

        doReturn(WriteQueue.FlushStatus.FLUSHING).when(transport).flushStatus();
        sut_.store(new StageContextMock<Void>(transport, null), Buffers.newCodecBuffer(0));
        TcpIOSelector.FlushTask task1 = flushTaskCaptor_.getValue();
        long secondWait = task1.execute(TimeUnit.MILLISECONDS);

        sut_.store(new StageContextMock<Void>(transport, null), Buffers.newCodecBuffer(0));
        TcpIOSelector.FlushTask task2 = flushTaskCaptor_.getValue();
        long thirdWait = task2.execute(TimeUnit.MILLISECONDS);

        assertThat(firstWait, is(WriteQueue.FlushStatus.FLUSHING.waitTimeMillis_));
        assertThat(secondWait, is(WriteQueue.FlushStatus.FLUSHED.waitTimeMillis_));
        assertThat(thirdWait, is(WriteQueue.FlushStatus.FLUSHED.waitTimeMillis_));
        verify(transport, times(3)).readyToWrite(Mockito.any(AttachedMessage.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStore_FollowingFlushReturnsFlushingOnceFlushingForTheSameFlushTask() throws Exception {
        NioClientSocketTransport transport = mock(NioClientSocketTransport.class);
        doReturn(WriteQueue.FlushStatus.FLUSHING).when(transport).flush();

        doReturn(null).when(transport).flushStatus();
        sut_.store(new StageContextMock<Void>(transport, null), Buffers.newCodecBuffer(0));
        TcpIOSelector.FlushTask task0 = flushTaskCaptor_.getValue();
        long firstWait = task0.execute(TimeUnit.MILLISECONDS);

        doReturn(WriteQueue.FlushStatus.FLUSHING).when(transport).flushStatus();
        long secondWait = task0.execute(TimeUnit.MILLISECONDS);
        long thirdWait = task0.execute(TimeUnit.MILLISECONDS);

        assertThat(firstWait, is(WriteQueue.FlushStatus.FLUSHING.waitTimeMillis_));
        assertThat(secondWait, is(WriteQueue.FlushStatus.FLUSHING.waitTimeMillis_));
        assertThat(thirdWait, is(WriteQueue.FlushStatus.FLUSHING.waitTimeMillis_));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStore_FollowingFlushReturnsFlushedIfFlushStatusChangesForTheSameFlushTask() throws Exception {
        NioClientSocketTransport transport = mock(NioClientSocketTransport.class);

        doReturn(WriteQueue.FlushStatus.FLUSHING).when(transport).flush();
        doReturn(null).when(transport).flushStatus();
        sut_.store(new StageContextMock<Void>(transport, null), Buffers.newCodecBuffer(0));

        TcpIOSelector.FlushTask task0 = flushTaskCaptor_.getValue();
        long firstWait = task0.execute(TimeUnit.MILLISECONDS);

        doReturn(WriteQueue.FlushStatus.FLUSHING).when(transport).flushStatus();
        long secondWait = task0.execute(TimeUnit.MILLISECONDS);

        doReturn(WriteQueue.FlushStatus.FLUSHED).when(transport).flush();
        long thirdWait = task0.execute(TimeUnit.MILLISECONDS);

        assertThat(firstWait, is(WriteQueue.FlushStatus.FLUSHING.waitTimeMillis_));
        assertThat(secondWait, is(WriteQueue.FlushStatus.FLUSHING.waitTimeMillis_));
        assertThat(thirdWait, is(WriteQueue.FlushStatus.FLUSHED.waitTimeMillis_));
        verify(transport).readyToWrite(Mockito.any(AttachedMessage.class));
    }

    @Test
    public void testProcessSelectedKeys_BufferIsNotDuplicatedWhenConnected() throws Exception {
        TcpIOSelector sut = new TcpIOSelector(256, false, false);
        SocketChannel channel = mock(SocketChannel.class);
        when(channel.read(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(10);
                return 10;
            }
        });
        NioSocketTransport<?> transport = mock(NioSocketTransport.class);
        SelectionKey key = spy(new SelectionKeyMock());
        when(key.channel()).thenReturn(channel);
        key.attach(transport);

        sut.processSelectedKeys(new HashSet<>(Arrays.asList(key)));

        ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
        verify(transport).loadEvent(captor.capture());
        CodecBuffer cb = captor.getValue();
        assertThat(cb, is(instanceOf(ByteBufferCodecBuffer.class)));
    }

    @Test
    public void testProcessSelectedKeys_BufferIsDuplicatedWhenConnected() throws Exception {
        TcpIOSelector sut = new TcpIOSelector(256, false, true);
        SocketChannel channel = mock(SocketChannel.class);
        when(channel.isConnected()).thenReturn(true);
        when(channel.read(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(10);
                return 10;
            }
        });
        NioSocketTransport<?> transport = mock(NioSocketTransport.class);
        SelectionKey key = spy(new SelectionKeyMock());
        when(key.channel()).thenReturn(channel);
        key.attach(transport);

        sut.processSelectedKeys(new HashSet<>(Arrays.asList(key)));

        ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
        verify(transport).loadEvent(captor.capture());
        CodecBuffer cb = captor.getValue();
        assertThat(cb, is(instanceOf(ArrayCodecBuffer.class)));
        assertThat(cb.remainingBytes(), is(10));
    }

    private static class SelectionKeyMock extends AbstractSelectionKey {
        @Override
        public SelectableChannel channel() {
            return null;
        }

        @Override
        public Selector selector() {
            return null;
        }

        @Override
        public int interestOps() {
            return 0;
        }

        @Override
        public SelectionKey interestOps(int ops) {
            return null;
        }

        @Override
        public int readyOps() {
            return 0;
        }
    }
}
