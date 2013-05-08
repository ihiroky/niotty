package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class ArrayChunkTest {

    private ArrayChunk sut_;

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Before
    public void setUp() {
        sut_ = new ArrayChunk(new byte[10], ArrayChunkFactory.INSTANCE);
    }

    @Test
    public void testRetain() throws Exception {
        sut_.initialize();

        byte[] buffer0 = sut_.retain();
        int retainCount0 = sut_.retainCount();
        byte[] buffer1 = sut_.retain();
        int retainCount1 = sut_.retainCount();

        assertThat(buffer0, is(sut_.buffer_));
        assertThat(retainCount0, is(2));
        assertThat(buffer1, is(sut_.buffer_));
        assertThat(retainCount1, is(3));
    }

    @Test
    public void testRetain_ExceptionIfNotInitialized() throws Exception {
        exceptionRule_.expect(IllegalStateException.class);
        exceptionRule_.expectMessage("this chunk is already released or not initialized yet.");

        sut_.retain();
    }

    @Test
    public void testSize() throws Exception {
        assertThat(sut_.size(), is(sut_.buffer_.length));
    }
}
