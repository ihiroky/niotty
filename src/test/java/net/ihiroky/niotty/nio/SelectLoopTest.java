package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class SelectLoopTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testStore() throws Exception {
        NioSocketTransport<?> transport = mock(NioSocketTransport.class);
        SelectLoop sut = spy(new SelectLoop(256, 256, false, false));
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

}
