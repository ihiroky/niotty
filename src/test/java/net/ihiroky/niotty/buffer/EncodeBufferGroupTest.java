package net.ihiroky.niotty.buffer;

import org.junit.Test;

import java.util.Iterator;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class EncodeBufferGroupTest {
    @Test
    public void testIterator() throws Exception {
        EncodeBufferGroup sut = new EncodeBufferGroup();
        sut.addFirst(Buffers.newEncodeBuffer(new byte[1], 0, 1));
        sut.addLast(Buffers.newEncodeBuffer(new byte[2], 0, 2));
        sut.addFirst(Buffers.newEncodeBuffer(new byte[3], 0, 3));
        sut.addLast(Buffers.newEncodeBuffer(new byte[4], 0, 4));

        Iterator<EncodeBuffer> i = sut.iterator();
        assertThat(i.hasNext(), is(true));
        assertThat(i.next().limitBytes(), is(3));
        assertThat(i.hasNext(), is(true));
        assertThat(i.next().limitBytes(), is(1));
        assertThat(i.hasNext(), is(true));
        assertThat(i.next().limitBytes(), is(2));
        assertThat(i.hasNext(), is(true));
        assertThat(i.next().limitBytes(), is(4));
        assertThat(i.hasNext(), is(false));
    }
}
