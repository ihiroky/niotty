package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class ByteBufferChunkTest {

    private ByteBufferChunk sut_;

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Before
    public void setUp() {
        sut_ = new ByteBufferChunk(ByteBuffer.allocate(10), ByteBufferChunkFactory.heap());
        sut_.ready();
    }

    @Test
    public void testRetain() throws Exception {
        ByteBuffer b0 = sut_.initialize();

        int retainCount0 = sut_.referenceCount();
        ByteBuffer b1 = sut_.retain();
        int retainCount1 = sut_.referenceCount();
        ByteBuffer b2 = sut_.retain();
        int retainCount2 = sut_.referenceCount();

        assertThat(retainCount0, is(1));
        assertThat(retainCount1, is(2));
        assertThat(retainCount2, is(3));
        assertThat(b0, is(not(sameInstance(b1))));
        assertThat(b0, is(not(sameInstance(b2))));
        assertThat(b1, is(not(sameInstance(b2))));
        assertThat(b1.array(), is(sameInstance(b0.array())));
        assertThat(b2.array(), is(sameInstance(b0.array())));
    }

    @Test
    public void testRetain_ExceptionIfNotInitialized() throws Exception {
        exceptionRule_.expect(IllegalStateException.class);
        exceptionRule_.expectMessage("this chunk is already released or not initialized yet.");

        sut_.retain();
    }

    @Test
    public void testSize() throws Exception {
        assertThat(sut_.size(), is(10));
    }

    @Test
    public void testClear() throws Throwable {
        ByteBufferChunk sut = new ByteBufferChunk(ByteBuffer.allocateDirect(10), ByteBufferChunkFactory.direct());

        sut.clear();
    }
}
