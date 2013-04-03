package net.ihiroky.niotty.buffer;

import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class BufferSinkListTest {

    @Test
    public void testDispose() throws Exception {
        BufferSink car = mock(BufferSink.class);
        doNothing().when(car).dispose();
        BufferSink cdr = mock(BufferSink.class);
        doNothing().when(cdr).dispose();

        BufferSinkList list = new BufferSinkList(car, cdr);
        list.dispose();

        verify(car, times(1)).dispose();
        verify(cdr, times(1)).dispose();
    }
}
