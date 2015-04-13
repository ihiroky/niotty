package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.*;
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

import static org.mockito.Mockito.*;

/**
 *
 */
public class NioSocketTransportTest {

    private NioSocketTransport sut_;
    private NioEventDispatcher selector_;

    @Before
    public void setUp() throws Exception {
        selector_ = mock(NioEventDispatcher.class);
        when(selector_.ioStage()).thenReturn(mock(Stage.class));
        @SuppressWarnings("unchecked")
        NioEventDispatcherGroup eventDispatcherGroup = mock(NioEventDispatcherGroup.class);
        when(eventDispatcherGroup.assign(Mockito.<EventDispatcherSelection>any())).thenReturn(selector_);
        sut_ = new Impl("NioSocketTransportTest", PipelineComposer.empty(), eventDispatcherGroup);
    }

    @Test
    public void testRegister_DirectlyIfInDispatcherThread() throws Exception {
        SelectableChannel channel = mock(SelectableChannel.class);
        when(selector_.isInDispatcherThread()).thenReturn(true);

        sut_.register(channel, SelectionKey.OP_ACCEPT);

        verify(selector_).register(channel, SelectionKey.OP_ACCEPT, sut_);
    }

    @Test
    public void testRegister_IndirectlyIfNotInDispatcherThread() throws Exception {
        SelectableChannel channel = mock(SelectableChannel.class);
        when(selector_.isInDispatcherThread()).thenReturn(false);

        sut_.register(channel, SelectionKey.OP_ACCEPT);

        verify(selector_, never()).register(channel, SelectionKey.OP_ACCEPT, sut_);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(selector_).offer(eventCaptor.capture());
        eventCaptor.getValue().execute();

        verify(selector_).register(channel, SelectionKey.OP_ACCEPT, sut_);
    }

    private static class Impl extends NioSocketTransport {

        Impl(String name, PipelineComposer pipelineComposer, NioEventDispatcherGroup eventDispatcherGroup) {
            super(name, pipelineComposer, eventDispatcherGroup);
        }

        @Override
        void onSelected(SelectionKey key, NioEventDispatcher selectDispatcher) {
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

        @Override
        public Pipeline pipeline() {
            return null;
        }

        @Override
        public int pendingWriteBuffers() {
            return 0;
        }
    }
}
