package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.Task;
import net.ihiroky.niotty.TaskLoopGroup;
import net.ihiroky.niotty.TaskSelection;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportOption;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.BufferSink;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class NioSocketTransportTest {

    private NioSocketTransport<SelectLoop> sut_;
    private SelectLoop selector_;

    @Before
    public void setUp() throws Exception {
        selector_ = mock(SelectLoop.class);
        @SuppressWarnings("unchecked")
        TaskLoopGroup<SelectLoop> taskLoopGroup = mock(TaskLoopGroup.class);
        when(taskLoopGroup.assign(Mockito.<TaskSelection>any())).thenReturn(selector_);
        sut_ = new Impl("NioSocketTransportTest", PipelineComposer.empty(), taskLoopGroup);
    }

    @Test
    public void testRegister_DirectlyIfInLoopThread() throws Exception {
        SelectableChannel channel = mock(SelectableChannel.class);
        LoadPipeline loadPipeline = mock(LoadPipeline.class);
        when(selector_.isInLoopThread()).thenReturn(true);

        sut_.register(channel, SelectionKey.OP_ACCEPT, loadPipeline);

        verify(selector_).register(channel, SelectionKey.OP_ACCEPT, sut_);

        ArgumentCaptor<TransportStateEvent> eventCaptor = ArgumentCaptor.forClass(TransportStateEvent.class);
        verify(loadPipeline).execute(eventCaptor.capture());
        assertThat(eventCaptor.getValue().state(), is(TransportState.INTEREST_OPS));
        assertThat(eventCaptor.getValue().value(), is((Object) SelectionKey.OP_ACCEPT));
    }

    @Test
    public void testRegister_IndirectlyIfNotInLoopThread() throws Exception {
        SelectableChannel channel = mock(SelectableChannel.class);
        LoadPipeline loadPipeline = mock(LoadPipeline.class);
        when(selector_.isInLoopThread()).thenReturn(false);

        sut_.register(channel, SelectionKey.OP_ACCEPT, loadPipeline);

        verify(selector_, never()).register(channel, SelectionKey.OP_ACCEPT, sut_);
        verify(loadPipeline, never()).execute(Mockito.<TransportStateEvent>any());

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(selector_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);

        ArgumentCaptor<TransportStateEvent> eventCaptor = ArgumentCaptor.forClass(TransportStateEvent.class);
        verify(selector_).register(channel, SelectionKey.OP_ACCEPT, sut_);
        verify(loadPipeline).execute(eventCaptor.capture());
        assertThat(eventCaptor.getValue().state(), is(TransportState.INTEREST_OPS));
        assertThat(eventCaptor.getValue().value(), is((Object) SelectionKey.OP_ACCEPT));
    }

    private static class Impl extends NioSocketTransport<SelectLoop> {

        Impl(String name, PipelineComposer pipelineComposer, TaskLoopGroup<SelectLoop> taskLoopGroup) {
            super(name, pipelineComposer, taskLoopGroup);
        }

        @Override
        void onSelected(SelectionKey key, SelectLoop selectLoop) {
        }

        @Override
        void readyToWrite(AttachedMessage<BufferSink> message) {
        }

        @Override
        void flush(ByteBuffer writeBuffer) throws IOException {
        }

        @Override
        public TransportFuture bind(SocketAddress local) {
            return null;
        }

        @Override
        public TransportFuture connect(SocketAddress local) {
            return null;
        }

        @Override
        public TransportFuture close() {
            return null;
        }

        @Override
        public SocketAddress localAddress() {
            return null;
        }

        @Override
        public SocketAddress remoteAddress() {
            return null;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public <T> Transport setOption(TransportOption<T> option, T value) {
            return null;
        }

        @Override
        public <T> T option(TransportOption<T> option) {
            return null;
        }

        @Override
        public Set<TransportOption<?>> supportedOptions() {
            return null;
        }
    }
}
