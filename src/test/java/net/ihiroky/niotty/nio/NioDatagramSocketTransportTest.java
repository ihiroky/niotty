package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.Task;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

/**
 *
 */
public class NioDatagramSocketTransportTest {

    private NioDatagramSocketTransport sut_;
    private WriteQueue writeQueue_;

    @Before
    public void setUp() throws Exception {
        UdpIOSelectorPool ioPool = mock(UdpIOSelectorPool.class);
        writeQueue_ = mock(WriteQueue.class);
        WriteQueueFactory writeQueueFactory = mock(WriteQueueFactory.class);
        when(writeQueueFactory.newWriteQueue()).thenReturn(writeQueue_);
        SelectionKey selectionKey = mock(SelectionKey.class);
        UdpIOSelector taskLoop = mock(UdpIOSelector.class);

        sut_ = spy(new NioDatagramSocketTransport(
                PipelineComposer.empty(), null, "TEST", ioPool, writeQueueFactory));
        when(sut_.taskLoop()).thenReturn(taskLoop);
        sut_.setSelectionKey(selectionKey);
    }

    @Test
    public void testFlush_Flushed() throws Exception {
        when(writeQueue_.flushTo(Mockito.any(DatagramChannel.class), Mockito.any(ByteBuffer.class)))
                .thenReturn(WriteQueue.FlushStatus.FLUSHED);
        when(sut_.key().interestOps()).thenReturn(0xFFFFFFFF);

        sut_.flush(ByteBuffer.allocate(0));

        ArgumentCaptor<Integer> opsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(sut_.key()).interestOps(opsCaptor.capture());
        assertThat(opsCaptor.getValue(), is(~SelectionKey.OP_WRITE));
        verify(sut_, never()).taskLoop();
    }

    @Test
    public void testFlush_Flushing() throws Exception {
        when(writeQueue_.flushTo(Mockito.any(DatagramChannel.class), Mockito.any(ByteBuffer.class)))
                .thenReturn(WriteQueue.FlushStatus.FLUSHING);
        when(sut_.key().interestOps()).thenReturn(SelectionKey.OP_WRITE);

        sut_.flush(null);
        when(sut_.key().interestOps()).thenReturn(0); // sut_.key() is mock, so set result explicitly.

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(sut_.taskLoop()).schedule(taskCaptor.capture(), anyLong(), Mockito.any(TimeUnit.class));
        Task task = taskCaptor.getValue();
        task.execute(TimeUnit.NANOSECONDS);

        ArgumentCaptor<Integer> opsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(sut_.key(), times(2)).interestOps(opsCaptor.capture());
        List<Integer> opsList = opsCaptor.getAllValues();
        assertThat(opsList.get(0), is(0));
        assertThat(opsList.get(1), is(SelectionKey.OP_WRITE));
    }

    @Test
    public void testFlush_SkipFlushingIfPreviousFlushIsFlushing() throws Exception {
        when(writeQueue_.flushTo(Mockito.any(DatagramChannel.class), Mockito.any(ByteBuffer.class)))
                .thenReturn(WriteQueue.FlushStatus.FLUSHING);
        when(sut_.key().interestOps()).thenReturn(SelectionKey.OP_WRITE);

        sut_.flush(null);
        sut_.flush(null);

        verify(writeQueue_).flushTo(Mockito.any(DatagramChannel.class), Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testFlush_Skipped() throws Exception {
        when(writeQueue_.flushTo(Mockito.any(DatagramChannel.class), Mockito.any(ByteBuffer.class)))
                .thenReturn(WriteQueue.FlushStatus.SKIPPED);
        when(sut_.key().interestOps()).thenReturn(0);

        sut_.flush(ByteBuffer.allocate(0));

        ArgumentCaptor<Integer> opsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(sut_.key()).interestOps(opsCaptor.capture());
        assertThat(opsCaptor.getValue(), is(SelectionKey.OP_WRITE));
        verify(sut_, never()).taskLoop();
    }
}
