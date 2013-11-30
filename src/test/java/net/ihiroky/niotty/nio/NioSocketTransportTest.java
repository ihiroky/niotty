package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.Stage;
import net.ihiroky.niotty.Task;
import net.ihiroky.niotty.TaskLoopGroup;
import net.ihiroky.niotty.TaskSelection;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportOption;
import net.ihiroky.niotty.buffer.Packet;
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
        when(selector_.ioStage()).thenReturn(mock(Stage.class));
        @SuppressWarnings("unchecked")
        TaskLoopGroup<SelectLoop> taskLoopGroup = mock(TaskLoopGroup.class);
        when(taskLoopGroup.assign(Mockito.<TaskSelection>any())).thenReturn(selector_);
        sut_ = new Impl("NioSocketTransportTest", PipelineComposer.empty(), taskLoopGroup);
    }

    @Test
    public void testRegister_DirectlyIfInLoopThread() throws Exception {
        SelectableChannel channel = mock(SelectableChannel.class);
        when(selector_.isInLoopThread()).thenReturn(true);

        sut_.register(channel, SelectionKey.OP_ACCEPT);

        verify(selector_).register(channel, SelectionKey.OP_ACCEPT, sut_);
    }

    @Test
    public void testRegister_IndirectlyIfNotInLoopThread() throws Exception {
        SelectableChannel channel = mock(SelectableChannel.class);
        when(selector_.isInLoopThread()).thenReturn(false);

        sut_.register(channel, SelectionKey.OP_ACCEPT);

        verify(selector_, never()).register(channel, SelectionKey.OP_ACCEPT, sut_);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(selector_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);

        verify(selector_).register(channel, SelectionKey.OP_ACCEPT, sut_);
    }

    private static class Impl extends NioSocketTransport<SelectLoop> {

        Impl(String name, PipelineComposer pipelineComposer, TaskLoopGroup<SelectLoop> taskLoopGroup) {
            super(name, pipelineComposer, taskLoopGroup);
        }

        @Override
        void onSelected(SelectionKey key, SelectLoop selectLoop) {
        }

        @Override
        void readyToWrite(Packet message, Object parameter) {
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
