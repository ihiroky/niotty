package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.Task;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
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
        when(sut_.key().interestOps()).thenReturn(0);
        when(sut_.taskLoop().schedule(Mockito.any(Task.class), anyLong(), Mockito.any(TimeUnit.class)))
                .thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        Task task = (Task) invocation.getArguments()[0];
                        task.execute(TimeUnit.NANOSECONDS);
                        return null;
                    }
                });

        sut_.flush(ByteBuffer.allocate(0));

        ArgumentCaptor<Integer> opsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(sut_.key()).interestOps(opsCaptor.capture());
        assertThat(opsCaptor.getValue(), is(SelectionKey.OP_WRITE));
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
