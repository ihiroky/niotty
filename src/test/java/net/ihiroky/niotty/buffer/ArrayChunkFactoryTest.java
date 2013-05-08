package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class ArrayChunkFactoryTest {

    ArrayChunkFactory sut_;

    @Before
    public void setUp() {
        sut_ = ArrayChunkFactory.INSTANCE;
    }

    @Test
    public void testNewChunk() throws Exception {
        Chunk<byte[]> chunk0 = sut_.newChunk(8);
        chunk0.initialize();
        chunk0.release();
        Chunk<byte[]> chunk1 = sut_.newChunk(8);

        assertThat(chunk0.size(), is(8));
        assertThat(chunk0.retainCount(), is(0));
        assertThat(chunk1.size(), is(8));
        assertThat(chunk0, is(not(sameInstance(chunk1))));
    }

    @Test
    public void testRelease() throws Exception {
        Chunk<byte[]> chunk0 = sut_.newChunk(8);
        chunk0.initialize();
        chunk0.release();

        // verify(sut_, times(1)).release(chunk0);
    }

    @Test
    public void testClose() throws Exception {
        // nothing happens.
        sut_.close();
    }
}
