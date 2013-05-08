package net.ihiroky.niotty.buffer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class ArrayChunkPoolTest {

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Test
    public void testConstructor_ExceptionIfMaxPoolingBytesIsZero() throws Exception {
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("maxPoolingBytes must be positive.");

        new ArrayChunkPool(0);
    }

    @Test
    public void testConstructor_ExceptionIfMaxPoolingBytesIsNegative() throws Exception {
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("maxPoolingBytes must be positive.");

        new ArrayChunkPool(-1);
    }

    @Test
    public void testAllocate() throws Exception {
        ArrayChunkPool sut = new ArrayChunkPool(10);

        ArrayChunk chunk0 = sut.allocate(6);
        int allocated0 = sut.allocatedBytes();
        ArrayChunk chunk1 = sut.allocate(5); // over maxPoolingBytes
        int allocated1 = sut.allocatedBytes();
        ArrayChunk chunk2 = sut.allocate(4);
        int allocated2 = sut.allocatedBytes();

        assertThat(chunk0.manager(), is((ChunkManager<byte[]>) sut));
        assertThat(chunk1.manager(), is((ChunkManager<byte[]>) ArrayChunkFactory.INSTANCE));
        assertThat(chunk2.manager(), is((ChunkManager<byte[]>) sut));
        assertThat(allocated0, is(6));
        assertThat(allocated1, is(6));
        assertThat(allocated2, is(10));
    }

    @Test
    public void testDispose() throws Exception {
        ArrayChunkPool sut = spy(new ArrayChunkPool(10));

        sut.close();

        verify(sut, times(1)).dispose();
    }
}
