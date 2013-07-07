package net.ihiroky.niotty.buffer;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class ByteBufferChunkFactoryTest {

    @Test
    public void testNewChunk_Heap() throws Exception {
        ByteBufferChunkFactory sut = ByteBufferChunkFactory.heap();

        ByteBufferChunk chunk = sut.newChunk(10);
        ByteBuffer buffer = chunk.initialize();

        assertThat(chunk.size(), is(10));
        assertThat(buffer.remaining(), is(10));
        assertThat(buffer.isDirect(), is(false));
        assertThat(buffer.hasArray(), is(true));
    }

    @Test
    public void testNewChunk_Direct() throws Exception {
        ByteBufferChunkFactory sut = ByteBufferChunkFactory.direct();

        ByteBufferChunk chunk = sut.newChunk(10);
        ByteBuffer buffer = chunk.initialize();

        assertThat(chunk.size(), is(10));
        assertThat(buffer.remaining(), is(10));
        assertThat(buffer.isDirect(), is(true));
        assertThat(buffer.hasArray(), is(false));
    }

    @Test
    public void testRelease_CallRelease() throws Throwable {
        ByteBufferChunk chunk = spy(
                new ByteBufferChunk(ByteBuffer.allocateDirect(10), ByteBufferChunkFactory.direct(true)));
        ByteBufferChunkFactory.direct(true).release(chunk);

        verify(chunk, times(1)).clear();
    }

    @Test
    public void testRelease_NotCallRelease() throws Throwable {
        ByteBufferChunk chunk0 = spy(
                new ByteBufferChunk(ByteBuffer.allocateDirect(10), ByteBufferChunkFactory.heap()));
        ByteBufferChunkFactory.heap().release(chunk0);
        ByteBufferChunk chunk1 = spy(
                new ByteBufferChunk(ByteBuffer.allocateDirect(10), ByteBufferChunkFactory.direct()));
        ByteBufferChunkFactory.direct().release(chunk1);

        verify(chunk0, never()).clear();
        verify(chunk1, never()).clear();
    }
}
