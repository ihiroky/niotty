package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.Task;
import net.ihiroky.niotty.TransportParameter;
import net.ihiroky.niotty.buffer.ArrayCodecBuffer;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.ByteBufferCodecBuffer;
import net.ihiroky.niotty.buffer.CodecBuffer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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
public class UdpIOSelectorTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testStore_FlushQueuing() throws Exception {
        NioDatagramSocketTransport transport = mock(NioDatagramSocketTransport.class);
        UdpIOSelector sut = spy(new UdpIOSelector(256, 256, false, false));
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Task task = (Task) invocation.getArguments()[0];
                task.execute(TimeUnit.NANOSECONDS);
                return null;
            }
        }).when(sut).offer(Mockito.any(Task.class));
        StageContext<Void> context = mock(StageContext.class);
        when(context.transport()).thenReturn(transport);
        when(context.transportParameter()).thenReturn(new DefaultTransportParameter(0));
        BufferSink data = Buffers.newCodecBuffer(0);

        sut.store(context, data);

        ArgumentCaptor<AttachedMessage> captor = ArgumentCaptor.forClass(AttachedMessage.class);
        verify(transport).readyToWrite(captor.capture());
        assertThat(captor.getValue().message(), is((Object) data));
        verify(transport).flush(Mockito.any(ByteBuffer.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testStore_FlushDirect() throws Exception {
        NioDatagramSocketTransport transport = mock(NioDatagramSocketTransport.class);
        UdpIOSelector sut = spy(new UdpIOSelector(256, 256, false, false));
        when(sut.isInLoopThread()).thenReturn(true);
        StageContext<Void> context = mock(StageContext.class);
        when(context.transport()).thenReturn(transport);
        when(context.transportParameter()).thenReturn(new DefaultTransportParameter(0));
        BufferSink data = Buffers.newCodecBuffer(0);

        sut.store(context, data);

        ArgumentCaptor<AttachedMessage> captor = ArgumentCaptor.forClass(AttachedMessage.class);
        verify(transport).readyToWrite(captor.capture());
        assertThat(captor.getValue().message(), is((Object) data));
        verify(transport).flush(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testProcessSelectedKeys_BufferIsNotDuplicatedWhenConnected() throws Exception {
        UdpIOSelector sut = new UdpIOSelector(256, 256, false, false);
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
        NioDatagramSocketTransport transport = mock(NioDatagramSocketTransport.class);
        LoadPipeline pipeline = mock(LoadPipeline.class);
        SelectionKey key = spy(new SelectionKeyMock());
        when(key.channel()).thenReturn(channel);
        when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
        when(transport.loadPipeline()).thenReturn(pipeline);
        key.attach(transport);

        sut.processSelectedKeys(new HashSet<SelectionKey>(Arrays.asList(key)));

        ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
        verify(transport.loadPipeline()).execute(captor.capture());
        CodecBuffer cb = captor.getValue();
        assertThat(cb, is(instanceOf(ByteBufferCodecBuffer.class)));
    }

    @Test
    public void testProcessSelectedKeys_BufferIsDuplicatedWhenConnected() throws Exception {
        UdpIOSelector sut = new UdpIOSelector(256, 256, false, true);
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
        NioDatagramSocketTransport transport = mock(NioDatagramSocketTransport.class);
        LoadPipeline pipeline = mock(LoadPipeline.class);
        SelectionKey key = spy(new SelectionKeyMock());
        when(key.channel()).thenReturn(channel);
        when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
        when(transport.loadPipeline()).thenReturn(pipeline);
        key.attach(transport);

        sut.processSelectedKeys(new HashSet<SelectionKey>(Arrays.asList(key)));

        ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
        verify(transport.loadPipeline()).execute(captor.capture());
        CodecBuffer cb = captor.getValue();
        assertThat(cb, is(instanceOf(ArrayCodecBuffer.class)));
        assertThat(cb.remainingBytes(), is(10));
    }

    @Test
    public void testProcessSelectedKeys_BufferIsNotDuplicatedWhenNotConnected() throws Exception {
        UdpIOSelector sut = new UdpIOSelector(256, 256, false, false);
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
        NioDatagramSocketTransport transport = mock(NioDatagramSocketTransport.class);
        LoadPipeline pipeline = mock(LoadPipeline.class);
        SelectionKey key = spy(new SelectionKeyMock());
        when(key.channel()).thenReturn(channel);
        when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
        when(transport.loadPipeline()).thenReturn(pipeline);
        key.attach(transport);

        sut.processSelectedKeys(new HashSet<SelectionKey>(Arrays.asList(key)));

        ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
        verify(transport.loadPipeline()).execute(captor.capture(), Mockito.any(TransportParameter.class));
        CodecBuffer cb = captor.getValue();
        assertThat(cb, is(instanceOf(ByteBufferCodecBuffer.class)));
    }

    @Test
    public void testProcessSelectedKeys_BufferIsDuplicatedWhenNotConnected() throws Exception {
        UdpIOSelector sut = new UdpIOSelector(256, 256, false, true);
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
        NioDatagramSocketTransport transport = mock(NioDatagramSocketTransport.class);
        LoadPipeline pipeline = mock(LoadPipeline.class);
        SelectionKey key = spy(new SelectionKeyMock());
        when(key.channel()).thenReturn(channel);
        when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
        when(transport.loadPipeline()).thenReturn(pipeline);
        key.attach(transport);

        sut.processSelectedKeys(new HashSet<SelectionKey>(Arrays.asList(key)));

        ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
        verify(transport.loadPipeline()).execute(captor.capture(), Mockito.any(TransportParameter.class));
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
