package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.Task;
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

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
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
public class TcpIOSelectorTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testStore_FlushIfNotOpWrite() throws Exception {
        NioClientSocketTransport transport = mock(NioClientSocketTransport.class);
        when(transport.containsInterestOp(anyInt())).thenReturn(false);
        TcpIOSelector sut = spy(new TcpIOSelector(256, false, false));
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Task task = (Task) invocation.getArguments()[0];
                task.execute(TimeUnit.NANOSECONDS);
                return null;
            }
        }).when(sut).execute(Mockito.any(Task.class));
        StageContext<Void> context = mock(StageContext.class);
        when(context.transport()).thenReturn(transport);
        when(context.transportParameter()).thenReturn(new DefaultTransportParameter(0));
        BufferSink data = Buffers.newCodecBuffer(0);

        sut.store(context, data);

        ArgumentCaptor<AttachedMessage> captor = ArgumentCaptor.forClass(AttachedMessage.class);
        verify(transport).readyToWrite(captor.capture());
        assertThat(captor.getValue().message(), is((Object) data));
        verify(transport).flush(null);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testStore_NotFlushIfOpWrite() throws Exception {
        NioClientSocketTransport transport = mock(NioClientSocketTransport.class);
        when(transport.containsInterestOp(anyInt())).thenReturn(true);
        TcpIOSelector sut = spy(new TcpIOSelector(256, false, false));
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Task task = (Task) invocation.getArguments()[0];
                task.execute(TimeUnit.NANOSECONDS);
                return null;
            }
        }).when(sut).execute(Mockito.any(Task.class));
        StageContext<Void> context = mock(StageContext.class);
        when(context.transport()).thenReturn(transport);
        when(context.transportParameter()).thenReturn(new DefaultTransportParameter(0));
        BufferSink data = Buffers.newCodecBuffer(0);

        sut.store(context, data);

        ArgumentCaptor<AttachedMessage> captor = ArgumentCaptor.forClass(AttachedMessage.class);
        verify(transport).readyToWrite(captor.capture());
        assertThat(captor.getValue().message(), is((Object) data));
        verify(transport, never()).flush(null);
    }

    @Test
    public void testProcessSelectedKeys_BufferIsNotDuplicatedWhenConnected() throws Exception {
        TcpIOSelector sut = new TcpIOSelector(256, false, false);
        SocketChannel channel = mock(SocketChannel.class);
        when(channel.read(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
                bb.position(10);
                return 10;
            }
        });
        NioSocketTransport<?> transport = mock(NioSocketTransport.class);
        SelectionKey key = spy(new SelectionKeyMock());
        when(key.channel()).thenReturn(channel);
        key.attach(transport);

        sut.processSelectedKeys(new HashSet<>(Arrays.asList(key)));

        ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
        verify(transport).loadEvent(captor.capture());
        CodecBuffer cb = captor.getValue();
        assertThat(cb, is(instanceOf(ByteBufferCodecBuffer.class)));
    }

    @Test
    public void testProcessSelectedKeys_BufferIsDuplicatedWhenConnected() throws Exception {
        TcpIOSelector sut = new TcpIOSelector(256, false, true);
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
        NioSocketTransport<?> transport = mock(NioSocketTransport.class);
        SelectionKey key = spy(new SelectionKeyMock());
        when(key.channel()).thenReturn(channel);
        key.attach(transport);

        sut.processSelectedKeys(new HashSet<>(Arrays.asList(key)));

        ArgumentCaptor<CodecBuffer> captor = ArgumentCaptor.forClass(CodecBuffer.class);
        verify(transport).loadEvent(captor.capture());
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
