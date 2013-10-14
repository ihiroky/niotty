package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.Task;
import net.ihiroky.niotty.TaskSelection;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportOptions;
import net.ihiroky.niotty.TransportParameter;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.ArrayCodecBuffer;
import net.ihiroky.niotty.buffer.ByteBufferCodecBuffer;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.JavaVersion;
import net.ihiroky.niotty.util.Platform;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static net.ihiroky.niotty.util.JavaVersionMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.Assume.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

/**
 *
 */
@RunWith(Enclosed.class)
public class NioDatagramSocketTransportTest {

    public static class FlushTest {

        private NioDatagramSocketTransport sut_;
        private WriteQueue writeQueue_;

        @Before
        public void setUp() throws Exception {
            SelectLoop selector = mock(SelectLoop.class);
            SelectLoopGroup ioPool = mock(SelectLoopGroup.class);
            when(ioPool.assign(Mockito.<TaskSelection>any())).thenReturn(selector);
            writeQueue_ = mock(WriteQueue.class);
            WriteQueueFactory writeQueueFactory = mock(WriteQueueFactory.class);
            when(writeQueueFactory.newWriteQueue()).thenReturn(writeQueue_);
            SelectionKey selectionKey = mock(SelectionKey.class);
            SelectLoop taskLoop = mock(SelectLoop.class);

            sut_ = spy(new NioDatagramSocketTransport(
                    "TEST", PipelineComposer.empty(), ioPool, writeQueueFactory, (InternetProtocolFamily) null));
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

    public static class OnSelectedTest {

        private SelectLoopGroup selectLoopGroup_;
        private WriteQueueFactory writeQueueFactory_;

        @Before
        public void setUp() throws Exception {
            writeQueueFactory_ = mock(WriteQueueFactory.class);
        }

        @After
        public void tearDown() throws Exception {
            if (selectLoopGroup_ != null) {
                selectLoopGroup_.close();
            }
        }

        @Test
        public void testReadBufferIsNotCopiedWhenConnected() throws Exception {
            selectLoopGroup_ = new SelectLoopGroup(Executors.defaultThreadFactory(), 1).setCopyReadBuffer(false);
            NioDatagramSocketTransport sut = spy(new NioDatagramSocketTransport("TEST", PipelineComposer.empty(),
                    selectLoopGroup_, writeQueueFactory_, (InternetProtocolFamily) null));

            DatagramChannel channel = mock(DatagramChannel.class);
            when(channel.isConnected()).thenReturn(true);
            when(channel.read(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
                @Override
                public Integer answer(InvocationOnMock invocation) throws Throwable {
                    ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                    bb.position(10);
                    return 10;
                }
            });

            LoadPipeline pipeline = mock(LoadPipeline.class);
            SelectionKey key = spy(new SelectionKeyMock());
            when(key.channel()).thenReturn(channel);
            when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
            when(sut.loadPipeline()).thenReturn(pipeline);
            key.attach(sut);
            sut.setSelectionKey(key);

            sut.onSelected(key, sut.taskLoop());

            ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
            verify(sut.loadPipeline()).execute(captor.capture());
            CodecBuffer cb = captor.getValue();
            assertThat(cb, is(instanceOf(ByteBufferCodecBuffer.class)));
        }

        @Test
        public void testReadBufferIsCopiedWhenConnected() throws Exception {
            selectLoopGroup_ = new SelectLoopGroup(Executors.defaultThreadFactory(), 1).setCopyReadBuffer(true);
            NioDatagramSocketTransport sut = spy(new NioDatagramSocketTransport("TEST", PipelineComposer.empty(),
                    selectLoopGroup_, writeQueueFactory_, (InternetProtocolFamily) null));

            DatagramChannel channel = mock(DatagramChannel.class);
            when(channel.isConnected()).thenReturn(true);
            when(channel.read(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
                @Override
                public Integer answer(InvocationOnMock invocation) throws Throwable {
                    ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                    bb.position(10);
                    return 10;
                }
            });
            LoadPipeline pipeline = mock(LoadPipeline.class);
            SelectionKey key = spy(new SelectionKeyMock());
            when(key.channel()).thenReturn(channel);
            when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
            when(sut.loadPipeline()).thenReturn(pipeline);
            key.attach(sut);
            sut.setSelectionKey(key);

            sut.onSelected(key, sut.taskLoop());

            ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
            verify(sut.loadPipeline()).execute(captor.capture());
            CodecBuffer cb = captor.getValue();
            assertThat(cb, is(instanceOf(ArrayCodecBuffer.class)));
            assertThat(cb.remainingBytes(), is(10));
        }

        @Test
        public void testReadBufferIsNotCopiedWhenNotConnected() throws Exception {
            selectLoopGroup_ = new SelectLoopGroup(Executors.defaultThreadFactory(), 1).setCopyReadBuffer(false);
            NioDatagramSocketTransport sut = spy(new NioDatagramSocketTransport("TEST", PipelineComposer.empty(),
                    selectLoopGroup_, writeQueueFactory_, (InternetProtocolFamily) null));

            DatagramChannel channel = mock(DatagramChannel.class);
            when(channel.isConnected()).thenReturn(false);
            when(channel.receive(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<SocketAddress>() {
                @Override
                public SocketAddress answer(InvocationOnMock invocation) throws Throwable {
                    ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                    bb.position(10);
                    return new InetSocketAddress(12345);
                }
            });
            LoadPipeline pipeline = mock(LoadPipeline.class);
            SelectionKey key = spy(new SelectionKeyMock());
            when(key.channel()).thenReturn(channel);
            when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
            when(sut.loadPipeline()).thenReturn(pipeline);
            key.attach(sut);
            sut.setSelectionKey(key);

            sut.onSelected(key, sut.taskLoop());

            ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
            verify(sut.loadPipeline()).execute(captor.capture(), Mockito.any(TransportParameter.class));
            CodecBuffer cb = captor.getValue();
            assertThat(cb, is(instanceOf(ByteBufferCodecBuffer.class)));
        }

        @Test
        public void testReadBufferIsCopiedWhenNotConnected() throws Exception {
            selectLoopGroup_ = new SelectLoopGroup(Executors.defaultThreadFactory(), 1).setCopyReadBuffer(true);
            NioDatagramSocketTransport sut = spy(new NioDatagramSocketTransport("TEST", PipelineComposer.empty(),
                    selectLoopGroup_, writeQueueFactory_, (InternetProtocolFamily) null));

            DatagramChannel channel = mock(DatagramChannel.class);
            when(channel.isConnected()).thenReturn(false);
            when(channel.receive(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<SocketAddress>() {
                @Override
                public SocketAddress answer(InvocationOnMock invocation) throws Throwable {
                    ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                    bb.position(10);
                    return new InetSocketAddress(12345);
                }
            });
            LoadPipeline pipeline = mock(LoadPipeline.class);
            SelectionKey key = spy(new SelectionKeyMock());
            when(key.channel()).thenReturn(channel);
            when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
            when(sut.loadPipeline()).thenReturn(pipeline);
            key.attach(sut);
            sut.setSelectionKey(key);

            sut.onSelected(key, sut.taskLoop());

            ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
            verify(sut.loadPipeline()).execute(captor.capture(), Mockito.any(TransportParameter.class));
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

    public static class Java7Test {

        private NioDatagramSocketTransport sut_;
        private DatagramChannel channel_;

        @Rule
        public ExpectedException exceptionRule_ = ExpectedException.none();

        @Before
        public void setUp() throws Exception {
            assumeThat(Platform.javaVersion(), is(greaterOrEqual(JavaVersion.JAVA7)));

            SelectLoop selector = mock(SelectLoop.class);
            SelectLoopGroup ioPool = mock(SelectLoopGroup.class);
            when(ioPool.assign(Mockito.<TaskSelection>any())).thenReturn(selector);
            DatagramSocket socket = mock(DatagramSocket.class);
            channel_ = mock(DatagramChannel.class);
            when(channel_.socket()).thenReturn(socket);
            WriteQueueFactory writeQueueFactory = mock(WriteQueueFactory.class);

            sut_ = new NioDatagramSocketTransport(
                    "TEST", PipelineComposer.empty(), ioPool, writeQueueFactory, channel_);
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
        public void testGetSoSndBuf() throws Exception {
            int expected = 10;
            when(channel_.getOption(StandardSocketOptions.SO_SNDBUF)).thenReturn(expected);

            int actual = sut_.option(TransportOptions.SO_SNDBUF);

            assertThat(actual, is(expected));
        }
        @Test
        public void testSetSoSndBuf() throws Exception {
            sut_.setOption(TransportOptions.SO_SNDBUF, 10);

            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(channel_).setOption(eq(StandardSocketOptions.SO_SNDBUF), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testGetSoBroadcast() throws Exception {
            boolean expected = true;
            when(channel_.getOption(StandardSocketOptions.SO_BROADCAST)).thenReturn(expected);

            boolean actual = sut_.option(TransportOptions.SO_BROADCAST);

            assertThat(actual, is(expected));
        }

        @Test
        public void testSetSoBroadcast() throws Exception {
            sut_.setOption(TransportOptions.SO_BROADCAST, true);

            ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(channel_).setOption(eq(StandardSocketOptions.SO_BROADCAST), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(true));
        }

        @Test
        public void testGetSoReuseAddress() throws Exception {
            boolean expected = true;
            when(channel_.getOption(StandardSocketOptions.SO_REUSEADDR)).thenReturn(expected);

            boolean actual = sut_.option(TransportOptions.SO_REUSEADDR);

            assertThat(actual, is(expected));
        }

        @Test
        public void testSetSoReuseAddress() throws Exception {
            sut_.setOption(TransportOptions.SO_REUSEADDR, true);

            ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(channel_).setOption(eq(StandardSocketOptions.SO_REUSEADDR), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(true));
        }

        @Test
        public void testGetIpMulticastIf() throws Exception {
            NetworkInterface expected = NetworkInterface.getByIndex(0);
            when(channel_.getOption(StandardSocketOptions.IP_MULTICAST_IF)).thenReturn(expected);

            NetworkInterface actual = sut_.option(TransportOptions.IP_MULTICAST_IF);

            assertThat(actual, is(expected));
        }

        @Test
        public void testSetIpMulticastIf() throws Exception {
            NetworkInterface ni = NetworkInterface.getByIndex(0);
            sut_.setOption(TransportOptions.IP_MULTICAST_IF, ni);

            ArgumentCaptor<NetworkInterface> valueCaptor = ArgumentCaptor.forClass(NetworkInterface.class);
            verify(channel_).setOption(eq(StandardSocketOptions.IP_MULTICAST_IF), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(ni));
        }

        @Test
        public void testGetIpMulticastLoop() throws Exception {
            boolean expected = true;
            when(channel_.getOption(StandardSocketOptions.IP_MULTICAST_LOOP)).thenReturn(expected);

            boolean actual = sut_.option(TransportOptions.IP_MULTICAST_LOOP);

            assertThat(actual, is(expected));
        }

        @Test
        public void testSetIpMulticastLoop() throws Exception {
            sut_.setOption(TransportOptions.IP_MULTICAST_LOOP, true);

            ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(channel_).setOption(eq(StandardSocketOptions.IP_MULTICAST_LOOP), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(true));
        }

        @Test
        public void testGetIpMulticastTtl() throws Exception {
            int expected = 10;
            when(channel_.getOption(StandardSocketOptions.IP_MULTICAST_TTL)).thenReturn(expected);

            int actual = sut_.option(TransportOptions.IP_MULTICAST_TTL);

            assertThat(actual, is(expected));
        }

        @Test
        public void testSetIpMulticastTtl() throws Exception {
            sut_.setOption(TransportOptions.IP_MULTICAST_TTL, 10);

            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(channel_).setOption(eq(StandardSocketOptions.IP_MULTICAST_TTL), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testGetIpTos() throws Exception {
            int expected = 10;
            when(channel_.getOption(StandardSocketOptions.IP_TOS)).thenReturn(expected);

            int actual = sut_.option(TransportOptions.IP_TOS);

            assertThat(actual, is(expected));
        }

        @Test
        public void testSetIpTos() throws Exception {
            sut_.setOption(TransportOptions.IP_TOS, 10);

            ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(channel_).setOption(eq(StandardSocketOptions.IP_TOS), valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(10));
        }

        @Test
        public void testGetUnsupportedOption() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("SO_LINGER");
            sut_.option(TransportOptions.SO_LINGER);
        }

        @Test
        public void testSetUnsupportedOption() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("SO_LINGER");
            sut_.setOption(TransportOptions.SO_LINGER, 10);
        }

        @Test
        public void testBind() throws Exception {
            InetSocketAddress endpoint = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 12345);
            StorePipeline storePipeline = spy(sut_.storePipeline());
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    TransportStateEvent event = (TransportStateEvent) invocation.getArguments()[0];
                    event.execute(TimeUnit.NANOSECONDS);
                    return null;
                }
            }).when(storePipeline).execute(Mockito.<TransportStateEvent>any());
            SelectLoop selector = mock(SelectLoop.class);
            when(selector.isInLoopThread()).thenReturn(true);
            NioDatagramSocketTransport sut = spy(sut_);
            when(sut.storePipeline()).thenReturn(storePipeline);

            sut.bind(endpoint);

            verify(channel_).bind(endpoint);
        }

        @Test
        public void testLocalAddress() throws Exception {
            InetSocketAddress expected = new InetSocketAddress(12345);
            when(channel_.getLocalAddress()).thenReturn(expected);

            InetSocketAddress actual = sut_.localAddress();

            assertThat(actual, is(expected));
        }

        @Test
        public void testRemoteAddress() throws Exception {
            InetSocketAddress expected = new InetSocketAddress(12345);
            when(channel_.getRemoteAddress()).thenReturn(expected);

            InetSocketAddress actual = sut_.remoteAddress();

            assertThat(actual, is(expected));
        }

        @Test
        public void testJoin_WithoutSource_Exception() throws Exception {
            InetAddress group = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByIndex(0);
            when(channel_.join(group, ni)).thenThrow(new IOException());

            TransportFuture future = sut_.join(group, ni);

            assertThat(future.throwable(), is(instanceOf(IOException.class)));
        }

        @Test
        public void testJoin_Exception() throws Exception {
            InetAddress group = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByIndex(0);
            InetAddress source = InetAddress.getByName("127.0.0.1");
            when(channel_.join(group, ni, source)).thenThrow(new IOException());

            TransportFuture future = sut_.join(group, ni, source);

            assertThat(future.throwable(), is(instanceOf(IOException.class)));
        }

        @Test
        public void testBlock() throws Exception {
            MembershipKey key0 = mock(MembershipKey.class);
            InetAddress group0 = InetAddress.getByName("225.0.0.1");
            NetworkInterface ni0 = NetworkInterface.getByIndex(0);
            when(channel_.join(group0, ni0)).thenReturn(key0);
            MembershipKey key1 = mock(MembershipKey.class);
            InetAddress group1 = InetAddress.getByName("225.0.0.2");
            NetworkInterface ni1 = NetworkInterface.getByIndex(0);
            when(channel_.join(group1, ni1)).thenReturn(key1);
            InetAddress source = InetAddress.getByName("127.0.0.1");

            sut_.join(group0, ni0);
            sut_.join(group1, ni1);
            sut_.block(group0, ni0, source);

            verify(key0).block(source);
            verify(key1, never()).block(source);
        }

        @Test
        public void testUnblock() throws Exception {
            MembershipKey key0 = mock(MembershipKey.class);
            InetAddress group0 = InetAddress.getByName("225.0.0.1");
            NetworkInterface ni0 = NetworkInterface.getByIndex(0);
            when(channel_.join(group0, ni0)).thenReturn(key0);
            MembershipKey key1 = mock(MembershipKey.class);
            InetAddress group1 = InetAddress.getByName("225.0.0.2");
            NetworkInterface ni1 = NetworkInterface.getByIndex(0);
            when(channel_.join(group1, ni1)).thenReturn(key1);
            InetAddress source = InetAddress.getByName("127.0.0.1");

            sut_.join(group0, ni0);
            sut_.join(group1, ni1);
            sut_.block(group0, ni0, source);
            sut_.block(group1, ni1, source);
            sut_.unblock(group0, ni0, source);

            verify(key0).unblock(source);
            verify(key1, never()).unblock(source);
        }
    }

    public static class Java6Test {
        private NioDatagramSocketTransport sut_;
        private DatagramSocket socket_;

        @Rule
        public ExpectedException exceptionRule_ = ExpectedException.none();

        @Before
        public void setUp() throws Exception {
            assumeThat(Platform.javaVersion(), is(equal(JavaVersion.JAVA6)));

            SelectLoop selector = mock(SelectLoop.class);
            SelectLoopGroup ioPool = mock(SelectLoopGroup.class);
            when(ioPool.assign(Mockito.<TaskSelection>any())).thenReturn(selector);
            socket_ = mock(DatagramSocket.class);
            DatagramChannel channel = mock(DatagramChannel.class);
            when(channel.socket()).thenReturn(socket_);
            WriteQueueFactory writeQueueFactory = mock(WriteQueueFactory.class);

            sut_ = new NioDatagramSocketTransport(
                    "TEST", PipelineComposer.empty(), ioPool, writeQueueFactory, channel);

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
        public void testGetSoSndBuf() throws Exception {
            int expected = 10;
            when(socket_.getSendBufferSize()).thenReturn(10);

            int actual = sut_.option(TransportOptions.SO_SNDBUF);

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
        public void testGetSoBroadcast() throws Exception {
            boolean expected = true;
            when(socket_.getBroadcast()).thenReturn(expected);

            boolean actual = sut_.option(TransportOptions.SO_BROADCAST);

            assertThat(actual, is(expected));
        }

        @Test
        public void testSetSoBroadcast() throws Exception {
            sut_.setOption(TransportOptions.SO_BROADCAST, true);

            ArgumentCaptor<Boolean> valueCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(socket_).setBroadcast(valueCaptor.capture());
            assertThat(valueCaptor.getValue(), is(true));
        }

        @Test
        public void testGetSoReuseAddress() throws Exception {
            boolean expected = true;
            when(socket_.getReuseAddress()).thenReturn(expected);

            boolean actual = sut_.option(TransportOptions.SO_REUSEADDR);

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
        public void testGetIpMulticastIf() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("Java 7 or later is required.");

            sut_.option(TransportOptions.IP_MULTICAST_IF);
        }

        @Test
        public void testSetIpMulticastIf() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("Java 7 or later is required.");

            NetworkInterface ni = NetworkInterface.getByName("lo");
            sut_.setOption(TransportOptions.IP_MULTICAST_IF, ni);
        }

        @Test
        public void testGetIpMulticastLoop() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("Java 7 or later is required.");

            sut_.option(TransportOptions.IP_MULTICAST_LOOP);
        }

        @Test
        public void testSetIpMulticastLoop() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("Java 7 or later is required.");

            sut_.setOption(TransportOptions.IP_MULTICAST_LOOP, true);
        }

        @Test
        public void testGetIpMulticastTtl() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("Java 7 or later is required.");

            sut_.option(TransportOptions.IP_MULTICAST_TTL);
        }

        @Test
        public void testSetIpMulticastTtl() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("Java 7 or later is required.");

            sut_.setOption(TransportOptions.IP_MULTICAST_TTL, 10);
        }

        @Test
        public void testGetIpTos() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("Java 7 or later is required.");

            sut_.option(TransportOptions.IP_TOS);
        }

        @Test
        public void testSetIpTos() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("Java 7 or later is required.");

            sut_.setOption(TransportOptions.IP_TOS, 10);
        }

        @Test
        public void testGetUnsupportedOption() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("SO_LINGER");
            sut_.option(TransportOptions.SO_LINGER);
        }

        @Test
        public void testSetUnsupportedOption() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("SO_LINGER");
            sut_.setOption(TransportOptions.SO_LINGER, 10);
        }

        @Test
        public void testBind() throws Exception {
            InetSocketAddress endpoint = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 12345);
            StorePipeline storePipeline = spy(sut_.storePipeline());
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    TransportStateEvent event = (TransportStateEvent) invocation.getArguments()[0];
                    event.execute(TimeUnit.NANOSECONDS);
                    return null;
                }
            }).when(storePipeline).execute(Mockito.<TransportStateEvent>any());
            SelectLoop selector = mock(SelectLoop.class);
            when(selector.isInLoopThread()).thenReturn(true);
            NioDatagramSocketTransport sut = spy(sut_);
            when(sut.storePipeline()).thenReturn(storePipeline);

            sut.bind(endpoint);

            verify(socket_).bind(endpoint);
        }

        @Test
        public void testLocalAddress() throws Exception {
            InetSocketAddress expected = new InetSocketAddress(12345);
            when(socket_.getLocalSocketAddress()).thenReturn(expected);

            InetSocketAddress actual = sut_.localAddress();

            assertThat(actual, is(expected));
        }

        @Test
        public void testRemoteAddress() throws Exception {
            InetSocketAddress expected = new InetSocketAddress(12345);
            when(socket_.getRemoteSocketAddress()).thenReturn(expected);

            InetSocketAddress actual = sut_.remoteAddress();

            assertThat(actual, is(expected));
        }

        @Test
        public void testJoin_WithoutSourceIsUnsupported() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("Java 7 or later is required.");
            InetAddress group = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByName("lo");

            sut_.join(group, ni);
        }

        @Test
        public void testJoin_IsUnsupported() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("Java 7 or later is required.");

            InetAddress group = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByName("lo");
            InetAddress source = InetAddress.getByName("127.0.0.1");

            sut_.join(group, ni, source);
        }

        @Test
        public void testBlock_IsUnsupported() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("Java 7 or later is required.");
            InetAddress group0 = InetAddress.getByName("225.0.0.1");
            NetworkInterface ni0 = NetworkInterface.getByName("lo");
            InetAddress source = InetAddress.getByName("127.0.0.1");

            sut_.block(group0, ni0, source);
        }

        @Test
        public void testUnblock_IsUnsupported() throws Exception {
            exceptionRule_.expect(UnsupportedOperationException.class);
            exceptionRule_.expectMessage("Java 7 or later is required.");
            InetAddress group0 = InetAddress.getByName("225.0.0.1");
            NetworkInterface ni0 = NetworkInterface.getByName("oo");
            InetAddress source = InetAddress.getByName("127.0.0.1");

            sut_.unblock(group0, ni0, source);
        }
    }
}
