package net.ihiroky.niotty.buffer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class ByteBufferChunkPoolTest {

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Test
    public void testConstructor_ExceptionIfMaxPoolingSizeIsZero() throws Exception {
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("maxPoolingBytes must be positive.");

        new ByteBufferChunkPool(0, false);
    }

    @Test
    public void testConstructor_ExceptionIfMaxPoolingSizeIsNegative() throws Exception {
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("maxPoolingBytes must be positive.");

        new ByteBufferChunkPool(-1, false);
    }

    @Test
    public void testAllocate() throws Exception {
        ByteBufferChunkPool sut = new ByteBufferChunkPool(10, true);
        try {
            ByteBufferChunk chunk0 = sut.allocate(6);
            int allocated0 = sut.wholeView().position();
            ByteBufferChunk chunk1 = sut.allocate(5); // over maxPoolingBytes
            int allocated1 = sut.wholeView().position();
            ByteBufferChunk chunk2 = sut.allocate(4);
            int allocated2 = sut.wholeView().position();

            assertThat(chunk0.manager(), is((ChunkManager<ByteBuffer>) sut));
            assertThat(chunk0.buffer_.isDirect(), is(true));
            assertThat(chunk1.manager(), is((ChunkManager<ByteBuffer>) ByteBufferChunkFactory.heap()));
            assertThat(chunk1.buffer_.isDirect(), is(false));
            assertThat(chunk2.manager(), is((ChunkManager<ByteBuffer>) sut));
            assertThat(chunk2.buffer_.isDirect(), is(true));
            assertThat(allocated0, is(6));
            assertThat(allocated1, is(6));
            assertThat(allocated2, is(10));
        } finally {
            sut.close();
        }
    }

    @Test
    public void testAllocate_Heap() throws Exception {
        ByteBufferChunkPool sut = new ByteBufferChunkPool(10, false);
        try {
            ByteBufferChunk chunk0 = sut.allocate(6);
            assertThat(chunk0.buffer_.isDirect(), is(false));
        } finally {
            sut.close();
        }
    }

    @Test
    public void testDispose() throws Exception {
        ByteBufferChunkPool sut = spy(new ByteBufferChunkPool(10, true));

        sut.close();

        verify(sut, times(1)).dispose();
    }
}
