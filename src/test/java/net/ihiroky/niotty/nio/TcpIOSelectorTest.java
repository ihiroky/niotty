package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.codec.StageContextMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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
        sut_ = spy(new TcpIOSelector(256, false));
        flushTaskCaptor_ = ArgumentCaptor.forClass(TcpIOSelector.FlushTask.class);
        doNothing().when(sut_).executeTask(flushTaskCaptor_.capture());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStore_Flushed() throws Exception {
        NioClientSocketTransport transport = mock(NioClientSocketTransport.class);
        doReturn(WriteQueue.FlushStatus.FLUSHED).when(transport).flush();

        sut_.store(new StageContextMock<Void>(transport, null), Buffers.newCodecBuffer(0));
        TcpIOSelector.FlushTask task = flushTaskCaptor_.getValue();
        task.execute(TimeUnit.MILLISECONDS);

        assertThat(task.flushStatus_.waitTimeMillis_, is(TaskLoop.DONE));
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

}
