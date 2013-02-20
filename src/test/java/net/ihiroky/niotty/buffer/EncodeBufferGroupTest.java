package net.ihiroky.niotty.buffer;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

    @Test
    public void testPeek() throws Exception {
        EncodeBufferGroup sut = new EncodeBufferGroup()
                .addLast(Buffers.newEncodeBuffer(new byte[1], 0, 1))
                .addLast(Buffers.newEncodeBuffer(new byte[2], 0, 2));
        assertThat(sut.peekFirst().limitBytes(), is(1));
        assertThat(sut.peekLast().limitBytes(), is(2));
    }

    @Test
    public void testFilledBytes() throws Exception {
        EncodeBufferGroup sut = new EncodeBufferGroup()
                .addLast(Buffers.newEncodeBuffer(new byte[1], 0, 1))
                .addLast(Buffers.newEncodeBuffer(new byte[2], 0, 2));
        assertThat(sut.filledBytes(), is(3));
    }

    @Test
    public void testTransferTo() throws Exception {
        EncodeBufferGroup sut = new EncodeBufferGroup()
                .addLast(Buffers.newEncodeBuffer(new byte[2], 0, 2))
                .addLast(Buffers.newEncodeBuffer(new byte[2], 0, 2));
        WritableByteChannel channel = mock(WritableByteChannel.class);
        ByteBuffer writeBuffer = ByteBuffer.allocate(2);
        when(channel.write(writeBuffer)).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ByteBuffer bb = (ByteBuffer) args[0];
                bb.position(bb.limit());
                return bb.position();
            }
        });

        assertThat(sut.remainingBytes(), is(4));
        boolean result = sut.transferTo(channel, writeBuffer);
        assertThat(result, is(true));
        assertThat(sut.remainingBytes(), is(0));
        verify(channel, times(2)).write(writeBuffer);
    }
}
