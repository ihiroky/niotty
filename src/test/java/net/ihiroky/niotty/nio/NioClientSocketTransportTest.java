package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.Pipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.Stage;
import net.ihiroky.niotty.Task;
import net.ihiroky.niotty.TaskSelection;
import net.ihiroky.niotty.TransportOptions;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.ArrayCodecBuffer;
import net.ihiroky.niotty.buffer.ByteBufferCodecBuffer;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.JavaVersion;
import net.ihiroky.niotty.util.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectionKey;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static net.ihiroky.niotty.util.JavaVersionMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.Assume.*;
import static org.mockito.Mockito.*;

/**
 *
 */
@RunWith(Enclosed.class)
public class NioClientSocketTransportTest {

    public static class OnSelectedTest {

        private SelectLoopGroup selectLoopGroup_;
        private WriteQueueFactory writeQueueFactory_;

        @Before
        public void setUp() throws Exception {
            writeQueueFactory_ = mock(WriteQueueFactory.class);
        }

        @Test
        public void testReadBufferIsNotCopiedWhenConnected() throws Exception {
            selectLoopGroup_ = new SelectLoopGroup(Executors.defaultThreadFactory(), 1).setCopyReadBuffer(false);
            NioClientSocketTransport sut = spy(new NioClientSocketTransport(
                    "TEST", PipelineComposer.empty(), selectLoopGroup_, writeQueueFactory_));

            SocketChannel channel = mock(SocketChannel.class);
            when(channel.read(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
                @Override
                public Integer answer(InvocationOnMock invocation) throws Throwable {
                    ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                    bb.position(10);
                    return 10;
                }
            });
            Pipeline pipeline = mock(Pipeline.class);
            SelectionKey key = spy(new SelectionKeyMock());
            when(key.channel()).thenReturn(channel);
            when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
            when(sut.pipeline()).thenReturn(pipeline);
            key.attach(sut);
            sut.setSelectionKey(key);

            sut.onSelected(key, sut.taskLoop());

            ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
            verify(sut.pipeline()).load(captor.capture());
            CodecBuffer cb = captor.getValue();
            assertThat(cb, is(instanceOf(ByteBufferCodecBuffer.class)));
        }

        @Test
        public void testReadBufferIsCopiedWhenConnected() throws Exception {
            selectLoopGroup_ = new SelectLoopGroup(Executors.defaultThreadFactory(), 1).setCopyReadBuffer(true);
            NioClientSocketTransport sut = spy(new NioClientSocketTransport(
                    "TEST", PipelineComposer.empty(), selectLoopGroup_, writeQueueFactory_));

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
            Pipeline pipeline = mock(Pipeline.class);
            SelectionKey key = spy(new SelectionKeyMock());
            when(key.channel()).thenReturn(channel);
            when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
            when(sut.pipeline()).thenReturn(pipeline);
            key.attach(sut);
            sut.setSelectionKey(key);

            sut.onSelected(key, sut.taskLoop());

            ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
            verify(sut.pipeline()).load(captor.capture());
            CodecBuffer cb = captor.getValue();
            assertThat(cb, is(instanceOf(ArrayCodecBuffer.class)));
            assertThat(cb.remaining(), is(10));
        }

        static class SelectionKeyMock extends AbstractSelectionKey {
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
    public static class FlushTest {
        private NioClientSocketTransport sut_;
        private SocketChannel channel_;
        private WriteQueue writeQueue_;

        @Before
        public void setUp() throws Exception {
            Stage stage = mock(Stage.class);
            SelectLoop selector = mock(SelectLoop.class);
            when(selector.ioStage()).thenReturn(stage);
            SelectLoopGroup ioPool = mock(SelectLoopGroup.class);
            when(ioPool.assign(Mockito.<TaskSelection>any())).thenReturn(selector);
            Socket socket = mock(Socket.class);
            channel_ = mock(SocketChannel.class);
            when(channel_.socket()).thenReturn(socket);
            writeQueue_ = mock(WriteQueue.class);
            WriteQueueFactory writeQueueFactory = mock(WriteQueueFactory.class);
            when(writeQueueFactory.newWriteQueue()).thenReturn(writeQueue_);
            SelectionKey selectionKey = mock(SelectionKey.class);
            SelectLoop taskLoop = mock(SelectLoop.class);

            sut_ = spy(new NioClientSocketTransport(
                    "TEST", PipelineComposer.empty(), ioPool, writeQueueFactory, channel_));
            when(sut_.taskLoop()).thenReturn(taskLoop);
            sut_.setSelectionKey(selectionKey);
        }

        @Test
        public void testFlush_Flushed() throws Exception {
            when(writeQueue_.flushTo(Mockito.any(GatheringByteChannel.class)))
                    .thenReturn(WriteQueue.FlushStatus.FLUSHED);
            when(sut_.key().interestOps()).thenReturn(0xFFFFFFFF);

            sut_.flush(null);

            ArgumentCaptor<Integer> opsCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(sut_.key()).interestOps(opsCaptor.capture());
            assertThat(opsCaptor.getValue(), is(~SelectionKey.OP_WRITE));
            verify(sut_, never()).taskLoop();
        }

        @Test
        public void testFlush_Flushing() throws Exception {
            when(writeQueue_.flushTo(Mockito.any(GatheringByteChannel.class)))
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
            when(writeQueue_.flushTo(Mockito.any(GatheringByteChannel.class)))
                    .thenReturn(WriteQueue.FlushStatus.FLUSHING);
            when(sut_.key().interestOps()).thenReturn(SelectionKey.OP_WRITE);

            sut_.flush(null);
            sut_.flush(null);

            verify(writeQueue_).flushTo(Mockito.any(GatheringByteChannel.class));
        }

        @Test
        public void testFlush_Skipped() throws Exception {
            when(writeQueue_.flushTo(Mockito.any(GatheringByteChannel.class)))
                    .thenReturn(WriteQueue.FlushStatus.SKIPPED);
            when(sut_.key().interestOps()).thenReturn(0);

            sut_.flush(null);

            ArgumentCaptor<Integer> opsCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(sut_.key()).interestOps(opsCaptor.capture());
            assertThat(opsCaptor.getValue(), is(SelectionKey.OP_WRITE));
            verify(sut_, never()).taskLoop();
        }
    }

    public static class Java7Test {
        private NioClientSocketTransport sut_;
        private SocketChannel channel_;

        @Before
        public void setUp() throws Exception {
            assumeThat(Platform.javaVersion(), is(greaterOrEqual(JavaVersion.JAVA7)));

            Stage ioStage = mock(Stage.class);
            SelectLoop selector = mock(SelectLoop.class);
            when(selector.ioStage()).thenReturn(ioStage);
            SelectLoopGroup ioPool = mock(SelectLoopGroup.class);
            when(ioPool.assign(Mockito.<TaskSelection>any())).thenReturn(selector);
            Socket socket = mock(Socket.class);
            channel_ = mock(SocketChannel.class);
            when(channel_.socket()).thenReturn(socket);
            WriteQueueFactory writeQueueFactory = mock(WriteQueueFactory.class);

            sut_ = new NioClientSocketTransport(
                    "TEST", PipelineComposer.empty(), ioPool, writeQueueFactory, channel_);
        }

        @Test
        public void testSetSoRcvBuf() throws Exception {
            sut_.setOption(TransportOptions.SO_RCVBUF, 10);

            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(channel_).setOption(eq(StandardSocketOptions.SO_RCVBUF), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testGetSoRcvBuf() throws Exception {
            when(channel_.getOption(StandardSocketOptions.SO_RCVBUF)).thenReturn(10);

            int actual = sut_.option(TransportOptions.SO_RCVBUF);

            verify(channel_).getOption(StandardSocketOptions.SO_RCVBUF);
            assertThat(actual, is(10));
        }

        @Test
        public void testSetSoSndBuf() throws Exception {
            sut_.setOption(TransportOptions.SO_SNDBUF, 10);

            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(channel_).setOption(eq(StandardSocketOptions.SO_SNDBUF), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testGetSoSndBuf() throws Exception {
            when(channel_.getOption(StandardSocketOptions.SO_SNDBUF)).thenReturn(10);

            int actual = sut_.option(TransportOptions.SO_SNDBUF);

            verify(channel_).getOption(StandardSocketOptions.SO_SNDBUF);
            assertThat(actual, is(10));
        }

        @Test
        public void testSetSoReuseAddress() throws Exception {
            sut_.setOption(TransportOptions.SO_REUSEADDR, true);

            ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(channel_).setOption(eq(StandardSocketOptions.SO_REUSEADDR), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(true));
        }

        @Test
        public void testGetSoReuseAddress() throws Exception {
            when(channel_.getOption(StandardSocketOptions.SO_REUSEADDR)).thenReturn(true);

            boolean actual = sut_.option(TransportOptions.SO_REUSEADDR);

            verify(channel_).getOption(StandardSocketOptions.SO_REUSEADDR);
            assertThat(actual, is(true));
        }

        @Test
        public void testSetSoKeepAlive() throws Exception {
            sut_.setOption(TransportOptions.SO_KEEPALIVE, true);

            ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(channel_).setOption(eq(StandardSocketOptions.SO_KEEPALIVE), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(true));
        }

        @Test
        public void testGetSoKeepAlive() throws Exception {
            when(channel_.getOption(StandardSocketOptions.SO_KEEPALIVE)).thenReturn(true);

            boolean actual = sut_.option(TransportOptions.SO_KEEPALIVE);

            verify(channel_).getOption(StandardSocketOptions.SO_KEEPALIVE);
            assertThat(actual, is(true));
        }

        @Test
        public void testSetSoLinger() throws Exception {
            sut_.setOption(TransportOptions.SO_LINGER, 10);

            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(channel_).setOption(eq(StandardSocketOptions.SO_LINGER), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testGetSoLinger() throws Exception {
            when(channel_.getOption(StandardSocketOptions.SO_LINGER)).thenReturn(10);

            int actual = sut_.option(TransportOptions.SO_LINGER);

            verify(channel_).getOption(StandardSocketOptions.SO_LINGER);
            assertThat(actual, is(10));
        }

        @Test
        public void testSetTcpNoDelay() throws Exception {
            sut_.setOption(TransportOptions.TCP_NODELAY, true);

            ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(channel_).setOption(eq(StandardSocketOptions.TCP_NODELAY), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(true));
        }

        @Test
        public void testGetTcpNoDelay() throws Exception {
            when(channel_.getOption(StandardSocketOptions.TCP_NODELAY)).thenReturn(true);

            boolean actual = sut_.option(TransportOptions.TCP_NODELAY);

            verify(channel_).getOption(StandardSocketOptions.TCP_NODELAY);
            assertThat(actual, is(true));
        }

        @Test(expected = UnsupportedOperationException.class)
        public void testSetUnsupportedOption() throws Exception {
            sut_.setOption(TransportOptions.SO_BROADCAST, true);
        }

        @Test(expected = UnsupportedOperationException.class)
        public void testGetUnsupportedOption() throws Exception {
            sut_.option(TransportOptions.SO_BROADCAST);
        }

        @Test
        public void testBind() throws Exception {
            InetSocketAddress bindAddress = new InetSocketAddress(1);
            SelectLoop selector = mock(SelectLoop.class);
            when(selector.isInLoopThread()).thenReturn(true);
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    TransportStateEvent event = (TransportStateEvent) invocation.getArguments()[0];
                    event.execute(TimeUnit.NANOSECONDS);
                    return null;
                }
            }).when(selector).execute(Mockito.<Task>any());
            NioClientSocketTransport sut = spy(sut_);
            when(sut.taskLoop()).thenReturn(selector);

            sut.bind(bindAddress);

            ArgumentCaptor<InetSocketAddress> captor = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(channel_).bind(captor.capture());
            assertThat((captor.getValue()), is(bindAddress));
        }

        @Test
        public void testLocalAddress() throws Exception {
            InetSocketAddress expected = new InetSocketAddress(1);
            when(channel_.getLocalAddress()).thenReturn(expected);

            InetSocketAddress actual = sut_.localAddress();

            verify(channel_).getLocalAddress();
            assertThat(actual, is(expected));
        }

        @Test
        public void testRemoteAddress() throws Exception {
            InetSocketAddress expected = new InetSocketAddress(1);
            when(channel_.getRemoteAddress()).thenReturn(expected);

            InetSocketAddress actual = sut_.remoteAddress();

            verify(channel_).getRemoteAddress();
            assertThat(actual, is(expected));
        }
    }

    public static class Java6Test {
        private NioClientSocketTransport sut_;
        private Socket socket_;

        @Before
        public void setUp() throws Exception {
            assumeThat(Platform.javaVersion(), is(equal(JavaVersion.JAVA6)));

            Stage ioStage = mock(Stage.class);
            SelectLoop selector = mock(SelectLoop.class);
            when(selector.ioStage()).thenReturn(ioStage);
            SelectLoopGroup ioPool = mock(SelectLoopGroup.class);
            when(ioPool.assign(Mockito.<TaskSelection>any())).thenReturn(selector);
            socket_ = mock(Socket.class);
            SocketChannel channel = mock(SocketChannel.class);
            when(channel.socket()).thenReturn(socket_);
            WriteQueueFactory writeQueueFactory = mock(WriteQueueFactory.class);

            sut_ = new NioClientSocketTransport(
                    "TEST", PipelineComposer.empty(), ioPool, writeQueueFactory, channel);

        }

        @Test
        public void testSetSoRcvBuf() throws Exception {
            sut_.setOption(TransportOptions.SO_RCVBUF, 10);

            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(socket_).setReceiveBufferSize(valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testGetSoRcvBuf() throws Exception {
            int expected = 10;
            when(socket_.getReceiveBufferSize()).thenReturn(expected);

            int actual = sut_.option(TransportOptions.SO_RCVBUF);

            verify(socket_).getReceiveBufferSize();
            assertThat(actual, is(expected));
        }

        @Test
        public void testSetSoSndBuf() throws Exception {
            sut_.setOption(TransportOptions.SO_SNDBUF, 10);

            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(socket_).setSendBufferSize(valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testGetSoSndBuf() throws Exception {
            int expected = 10;
            when(socket_.getSendBufferSize()).thenReturn(expected);

            int actual = sut_.option(TransportOptions.SO_SNDBUF);

            verify(socket_).getSendBufferSize();
            assertThat(actual, is(expected));
        }

        @Test
        public void testSetSoReuseAddress() throws Exception {
            sut_.setOption(TransportOptions.SO_REUSEADDR, true);

            ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(socket_).setReuseAddress(valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(true));
        }

        @Test
        public void testGetSoReuseAddress() throws Exception {
            boolean expected = true;
            when(socket_.getReuseAddress()).thenReturn(expected);

            boolean actual = sut_.option(TransportOptions.SO_REUSEADDR);

            verify(socket_).getReuseAddress();
            assertThat(actual, is(expected));
        }

        @Test
        public void testSetSoKeepAlive() throws Exception {
            sut_.setOption(TransportOptions.SO_KEEPALIVE, true);

            ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(socket_).setKeepAlive(valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(true));
        }

        @Test
        public void testGetSoKeepAlive() throws Exception {
            boolean expected = true;
            when(socket_.getKeepAlive()).thenReturn(expected);

            boolean actual = sut_.option(TransportOptions.SO_KEEPALIVE);

            verify(socket_).getKeepAlive();
            assertThat(actual, is(expected));
        }

        @Test
        public void testSetSoLinger() throws Exception {
            sut_.setOption(TransportOptions.SO_LINGER, 10);

            ArgumentCaptor<Boolean> flagCaptor = ArgumentCaptor.forClass(Boolean.class);
            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(socket_).setSoLinger(flagCaptor.capture(), valueCaptor.capture());
            assertThat(flagCaptor.getValue(), is(true));
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testGetSoLinger() throws Exception {
            int expected= 10;
            when(socket_.getSoLinger()).thenReturn(expected);

            int actual = sut_.option(TransportOptions.SO_LINGER);

            verify(socket_).getSoLinger();
            assertThat(actual, is(expected));
        }

        @Test
        public void testSetTcpNoDelay() throws Exception {
            sut_.setOption(TransportOptions.TCP_NODELAY, true);

            ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(socket_).setTcpNoDelay(valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(true));
        }

        @Test
        public void testGetTcpNoDelay() throws Exception {
            boolean expected = true;
            when(socket_.getTcpNoDelay()).thenReturn(expected);

            boolean actual = sut_.option(TransportOptions.TCP_NODELAY);

            verify(socket_).getTcpNoDelay();
            assertThat(actual, is(expected));
        }

        @Test(expected = UnsupportedOperationException.class)
        public void testSetUnsupportedOption() throws Exception {
            sut_.setOption(TransportOptions.SO_BROADCAST, true);
        }

        @Test(expected = UnsupportedOperationException.class)
        public void testGetUnsupportedOption() throws Exception {
            sut_.option(TransportOptions.SO_BROADCAST);
        }

        @Test
        public void testBind() throws Exception {
            InetSocketAddress bindAddress = new InetSocketAddress(1);
            SelectLoop selector = mock(SelectLoop.class);
            when(selector.isInLoopThread()).thenReturn(true);
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    TransportStateEvent event = (TransportStateEvent) invocation.getArguments()[0];
                    event.execute(TimeUnit.NANOSECONDS);
                    return null;
                }
            }).when(selector).execute(Mockito.<Task>any());
            NioClientSocketTransport sut = spy(sut_);
            when(sut.taskLoop()).thenReturn(selector);

            sut.bind(bindAddress);

            ArgumentCaptor<InetSocketAddress> captor = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket_).bind(captor.capture());
            assertThat((captor.getValue()), is(bindAddress));
        }

        @Test
        public void testLocalAddress() throws Exception {
            InetSocketAddress expected = new InetSocketAddress(1);
            when(socket_.getLocalSocketAddress()).thenReturn(expected);

            InetSocketAddress actual = sut_.localAddress();

            verify(socket_).getLocalSocketAddress();
            assertThat(actual, is(expected));
        }

        @Test
        public void testRemoteAddress() throws Exception {
            InetSocketAddress expected = new InetSocketAddress(1);
            when(socket_.getRemoteSocketAddress()).thenReturn(expected);

            InetSocketAddress actual = sut_.remoteAddress();

            verify(socket_).getRemoteSocketAddress();
            assertThat(actual, is(expected));
        }
    }
}
