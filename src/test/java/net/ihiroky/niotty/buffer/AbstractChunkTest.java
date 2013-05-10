package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class AbstractChunkTest {

    private AbstractChunk<Integer> sut_;

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        ChunkManager<Integer> chunkManager = mock(ChunkManager.class);
        sut_ = new AbstractChunk<Integer>(0, chunkManager) {
            @Override
            public Integer retain() {
                return buffer_;
            }
            @Override
            public int size() {
                return 1;
            }
        };
        sut_.ready();
    }

    @Test
    public void testInitialize() throws Exception {
        int retainCountBefore = sut_.referenceCount();
        int buffer = sut_.initialize();
        int retainCountAfter = sut_.referenceCount();

        assertThat(retainCountBefore, is(-1));
        assertThat(retainCountAfter, is(1));
        assertThat(buffer, is(0));
    }

    @Test
    public void testInitialize_ExceptionIfAfterRelease() throws Exception {
        exceptionRule_.expect(IllegalStateException.class);
        exceptionRule_.expectMessage("this chunk is not in the pre-initialized state.");

        sut_.initialize();
        sut_.release();
        sut_.initialize();
    }

    @Test
    public void testInitialize_OkIfReleasedAndThenReady() throws Exception {
        sut_.initialize();
        sut_.release();
        sut_.ready();
        sut_.initialize();

        assertThat(sut_.referenceCount(), is(1));
    }

    @Test
    public void testIncrementRetainCount() throws Exception {
        sut_.initialize();

        int retainCount0 = sut_.incrementRetainCount();
        int retainCount1 = sut_.incrementRetainCount();

        assertThat(retainCount0, is(2));
        assertThat(retainCount1, is(3));
    }

    @Test
    public void testIncrementCount_ExceptionIfRetainCountIsZero() throws Exception {
        exceptionRule_.expect(IllegalStateException.class);
        exceptionRule_.expectMessage("this chunk is already released or not initialized yet.");
        sut_.incrementRetainCount();
    }

    @Test
    public void testRelease() throws Exception {
        sut_.initialize();
        sut_.incrementRetainCount();

        int retainCount0 = sut_.release();
        int retainCount1 = sut_.release();

        assertThat(retainCount0, is(1));
        assertThat(retainCount1, is(0));
        verify(sut_.manager(), times(1)).release(sut_);
    }

    @Test
    public void testRelease_ExceptionIfRetainCountIsZero() throws Exception {
        exceptionRule_.expect(IllegalStateException.class);
        exceptionRule_.expectMessage("this chunk is already released or not initialized yet.");

        sut_.release();
    }

    @Test
    public void testReallocate() throws Exception {
        sut_.initialize();

        Chunk<Integer> newChunk = sut_.reallocate(-1);

        assertThat(sut_.referenceCount(), is(0));
        verify(sut_.manager(), times(1)).newChunk(-1);
        verify(sut_.manager(), times(1)).release(sut_);
        assertThat(newChunk, is(not(sameInstance((Chunk<Integer>) sut_))));
    }
}
