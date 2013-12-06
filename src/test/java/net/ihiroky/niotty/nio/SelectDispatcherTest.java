package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.Stage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.Packet;
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
public class SelectDispatcherTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testStore() throws Exception {
        NioSocketTransport<?> transport = mock(NioSocketTransport.class);
        SelectDispatcher selectDispatcher = spy(new SelectDispatcher(256, 256, false));
        Stage sut = selectDispatcher.ioStage();
        StageContext context = mock(StageContext.class);
        when(context.transport()).thenReturn(transport);
        Packet data = Buffers.newCodecBuffer(0);

        sut.stored(context, data, new Object());

        ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
        verify(transport).readyToWrite(captor.capture(), Mockito.<Object>any());
        assertThat(captor.getValue(), is(data));
        verify(transport).flush(Mockito.any(ByteBuffer.class));
    }

}
