package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Queue;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class ChunkPoolTest {

    private ChunkPool<byte[]> sut_;

    @Before
    public void setUp() {
        sut_ = new ArrayChunkPool(128);
    }

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Test
    public void testNewChunk() throws Exception {
        Chunk<byte[]> c7 = sut_.newChunk(7);
        Chunk<byte[]> c8 = sut_.newChunk(8);
        Chunk<byte[]> c9 = sut_.newChunk(9);
        Chunk<byte[]> c15 = sut_.newChunk(15);
        Chunk<byte[]> c16 = sut_.newChunk(16);
        Chunk<byte[]> c17 = sut_.newChunk(17);

        assertThat(c7.size(), is(8));
        assertThat(c8.size(), is(8));
        assertThat(c9.size(), is(16));
        assertThat(c15.size(), is(16));
        assertThat(c16.size(), is(16));
        assertThat(c17.size(), is(32));
    }

    @Test
    public void testRelease() throws Exception {
        Chunk<byte[]> c7 = sut_.newChunk(7);
        Chunk<byte[]> c8 = sut_.newChunk(8);
        Chunk<byte[]> c9 = sut_.newChunk(9);
        Chunk<byte[]> c15 = sut_.newChunk(15);
        Chunk<byte[]> c16 = sut_.newChunk(16);
        Chunk<byte[]> c17 = sut_.newChunk(17);

        c7.initialize();
        c7.release();
        c8.initialize();
        c8.release();
        c9.initialize();
        c9.release();
        c15.initialize();
        c15.release();
        c16.initialize();
        c16.release();
        c17.initialize();
        c17.release();

        Queue<Chunk<byte[]>>[] pools = sut_.pools();
        assertThat(pools[2].size(), is(0));
        assertThat(pools[3].poll(), is(c7));
        assertThat(pools[3].poll(), is(c8));
        assertThat(pools[3].size(), is(0));
        assertThat(pools[4].poll(), is(c9));
        assertThat(pools[4].poll(), is(c15));
        assertThat(pools[4].poll(), is(c16));
        assertThat(pools[4].size(), is(0));
        assertThat(pools[5].poll(), is(c17));
        assertThat(pools[5].size(), is(0));
        assertThat(pools[6].size(), is(0));
    }

    @Test
    public void testPool() throws Exception {
        Chunk<byte[]> c7 = sut_.newChunk(7);
        c7.initialize();
        c7.release();
        Chunk<byte[]> c8 = sut_.newChunk(8);
        c8.initialize();
        c8.release();

        assertThat(c7, is(sameInstance(c8)));
        assertThat(sut_.pools()[3].size(), is(1));
    }

    @Test
    public void testClose() throws Exception {
        ChunkPool<byte[]> sut = spy(sut_);
        doNothing().when(sut).dispose();

        Chunk<byte[]> c7 = sut.newChunk(7);
        Chunk<byte[]> c8 = sut.newChunk(8);
        c7.initialize();
        c8.initialize();
        c7.release();
        c8.release();
        sut.close();

        assertThat(sut.pools()[3].size(), is(0));
        verify(sut, times(1)).dispose();
    }

    @Test
    public void testClose_ExceptionIfUsedChunkExists() throws Exception {
        exceptionRule_.expect(IllegalStateException.class);
        exceptionRule_.expectMessage("1 chunks are still in use.");

        Chunk<byte[]> c0 = sut_.newChunk(10);
        Chunk<byte[]> c1 = sut_.newChunk(10);
        c0.initialize();
        c0.release();

        sut_.close();
    }
}
