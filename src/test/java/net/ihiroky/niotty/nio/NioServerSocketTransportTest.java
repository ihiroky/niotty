package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.Task;
import net.ihiroky.niotty.TransportOptions;
import net.ihiroky.niotty.util.JavaVersion;
import net.ihiroky.niotty.util.Platform;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
public class NioServerSocketTransportTest {
    public static class Java7Test {

        private NioServerSocketTransport sut_;
        private ServerSocketChannel channel_;
        private SelectLoopGroup acceptGroup_;
        private SelectLoopGroup ioGroup_;

        @Before
        public void setUp() {
            assumeThat(Platform.javaVersion(), is(greaterOrEqual(JavaVersion.JAVA7)));
            channel_ = mock(ServerSocketChannel.class);
            acceptGroup_ = new SelectLoopGroup(Executors.defaultThreadFactory(), 1);
            ioGroup_ = new SelectLoopGroup(Executors.defaultThreadFactory(), 1);
            WriteQueueFactory writeQueueFactory = mock(WriteQueueFactory.class);
            sut_ = new NioServerSocketTransport("TEST", PipelineComposer.empty(),
                    acceptGroup_, ioGroup_, writeQueueFactory, channel_);
        }

        @After
        public void tearDown() throws Exception {
            if (acceptGroup_ != null) {
                acceptGroup_.close();
            }
            if (ioGroup_ != null) {
                ioGroup_.close();
            }
        }

        @Test
        public void testGetSoRcvBuf() throws Exception {
            int expected = 10;
            when(channel_.getOption(StandardSocketOptions.SO_RCVBUF)).thenReturn(expected);

            int actual = sut_.option(TransportOptions.SO_RCVBUF);

            assertThat(actual, is(expected));
        }

        @Test
        public void testSetSoRcvBuf() throws Exception {
            sut_.setOption(TransportOptions.SO_RCVBUF, 10);

            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(channel_).setOption(eq(StandardSocketOptions.SO_RCVBUF), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testGetReuseAddress() throws Exception {
            boolean expected = true;
            when(channel_.getOption(StandardSocketOptions.SO_REUSEADDR)).thenReturn(expected);

            boolean actual = sut_.option(TransportOptions.SO_REUSEADDR);

            assertThat(actual, is(expected));
        }

        @Test
        public void testSetReuseAddress() throws Exception {
            sut_.setOption(TransportOptions.SO_REUSEADDR, true);

            ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(channel_).setOption(eq(StandardSocketOptions.SO_REUSEADDR), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(true));
        }

        @Test(expected = UnsupportedOperationException.class)
        public void testGetUnsupportedOption() throws Exception {
            sut_.option(TransportOptions.SO_SNDBUF);
        }

        @Test(expected = UnsupportedOperationException.class)
        public void testSetUnsupportedOption() throws Exception {
            sut_.setOption(TransportOptions.SO_SNDBUF, 10);
        }

        @Test
        public void testSetAcceptedTransportOption() throws Exception {
            sut_.setAcceptedTransportOption(TransportOptions.SO_LINGER, 10);

            SocketChannel acceptedChannel = mock(SocketChannel.class);

            sut_.register(acceptedChannel);

            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(acceptedChannel).setOption(eq(StandardSocketOptions.SO_LINGER), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testBind() throws Exception {
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", 12345);
            SelectLoop selectLoop = mock(SelectLoop.class);
            when(selectLoop.isInLoopThread()).thenReturn(true);
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    Task event = (Task) invocation.getArguments()[0];
                    event.execute(TimeUnit.NANOSECONDS);
                    return null;
                }
            }).when(selectLoop).execute(Mockito.<Task>any());
            NioServerSocketTransport sut = spy(sut_);
            when(sut.taskLoop()).thenReturn(selectLoop);

            sut.bind(address);

            ArgumentCaptor<InetSocketAddress> valueCaptor = ArgumentCaptor.forClass(InetSocketAddress.class);
            ArgumentCaptor<Integer> backlogCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(channel_).bind(valueCaptor.capture(), backlogCaptor.capture());
            assertThat(valueCaptor.getValue(), is(address));
            assertThat(backlogCaptor.getValue(), is(0));
        }

        @Test
        public void testLocalAddress() throws Exception {
            InetSocketAddress expected = new InetSocketAddress("127.0.0.1", 12345);
            when(channel_.getLocalAddress()).thenReturn(expected);

            InetSocketAddress actual = sut_.localAddress();

            assertThat(actual, is(expected));
        }
    }

    public static class Java6Test {

        private NioServerSocketTransport sut_;
        private ServerSocket socket_;
        private SelectLoopGroup acceptGroup_;
        private SelectLoopGroup ioGroup_;

        @Before
        public void setUp() {
            assumeThat(Platform.javaVersion(), is(equal(JavaVersion.JAVA6)));

            socket_ = mock(ServerSocket.class);
            ServerSocketChannel channel = mock(ServerSocketChannel.class);
            when(channel.socket()).thenReturn(socket_);
            acceptGroup_ = new SelectLoopGroup(Executors.defaultThreadFactory(), 1);
            ioGroup_ = new SelectLoopGroup(Executors.defaultThreadFactory(), 1);
            WriteQueueFactory writeQueueFactory = mock(WriteQueueFactory.class);
            sut_ = new NioServerSocketTransport("TEST", PipelineComposer.empty(), acceptGroup_, ioGroup_,
                    writeQueueFactory, channel);

        }

        @After
        public void tearDown() throws Exception {
            if (acceptGroup_ != null) {
                acceptGroup_.close();
            }
            if (ioGroup_ != null) {
                ioGroup_.close();
            }
        }

        @Test
        public void testGetSoRcvBuf() throws Exception {
            int expected = 10;
            when(socket_.getReceiveBufferSize()).thenReturn(expected);

            int actual = sut_.option(TransportOptions.SO_RCVBUF);

            assertThat(actual, is(expected));
        }

        @Test
        public void testSetSoRcvBuf() throws Exception {
            sut_.setOption(TransportOptions.SO_RCVBUF, 10);

            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(socket_).setReceiveBufferSize(valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testGetReuseAddress() throws Exception {
            boolean expected = true;
            when(socket_.getReuseAddress()).thenReturn(expected);

            boolean actual = sut_.option(TransportOptions.SO_REUSEADDR);

            assertThat(actual, is(expected));
        }

        @Test
        public void testSetReuseAddress() throws Exception {
            sut_.setOption(TransportOptions.SO_REUSEADDR, true);

            ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(socket_).setReuseAddress(valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(true));
        }

        @Test(expected = UnsupportedOperationException.class)
        public void testGetUnsupportedOption() throws Exception {
            sut_.option(TransportOptions.SO_SNDBUF);
        }

        @Test(expected = UnsupportedOperationException.class)
        public void testSetUnsupportedOption() throws Exception {
            sut_.setOption(TransportOptions.SO_SNDBUF, 10);
        }

        @Test
        public void testSetAcceptedTransportOption() throws Exception {
            sut_.setAcceptedTransportOption(TransportOptions.SO_LINGER, 10);
            Socket socket = mock(Socket.class);
            SocketChannel acceptedChannel = mock(SocketChannel.class);
            when(acceptedChannel.socket()).thenReturn(socket);

            sut_.register(acceptedChannel);

            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(socket).setSoLinger(eq(true), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testBind() throws Exception {
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", 12345);
            SelectLoop selectLoop = mock(SelectLoop.class);
            when(selectLoop.isInLoopThread()).thenReturn(true);
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    Task event = (Task) invocation.getArguments()[0];
                    event.execute(TimeUnit.NANOSECONDS);
                    return null;
                }
            }).when(selectLoop).execute(Mockito.<Task>any());
            NioServerSocketTransport sut = spy(sut_);
            when(sut.taskLoop()).thenReturn(selectLoop);

            sut.bind(address);

            ArgumentCaptor<InetSocketAddress> valueCaptor = ArgumentCaptor.forClass(InetSocketAddress.class);
            ArgumentCaptor<Integer> backlogCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(socket_).bind(valueCaptor.capture(), backlogCaptor.capture());
            assertThat(valueCaptor.getValue(), is(address));
            assertThat(backlogCaptor.getValue(), is(0));
        }

        @Test
        public void testLocalAddress() throws Exception {
            InetSocketAddress expected = new InetSocketAddress("127.0.0.1", 12345);
            when(socket_.getLocalSocketAddress()).thenReturn(expected);

            InetSocketAddress actual = sut_.localAddress();

            assertThat(actual, is(expected));
        }

    }
}
