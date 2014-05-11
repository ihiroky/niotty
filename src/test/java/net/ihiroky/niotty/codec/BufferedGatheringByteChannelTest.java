package net.ihiroky.niotty.codec;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BufferedGatheringByteChannelTest {

    BufferedGatheringByteChannel sut_;
    GatheringByteChannel baseChannel_;

    @Before
    public void setUp() throws Exception {
        baseChannel_ = mock(GatheringByteChannel.class);

        sut_ = new BufferedGatheringByteChannel(32, false);
        sut_.setUnderlyingChannel(baseChannel_);
    }

    @After
    public void tearDown() throws Exception {
        sut_.close();
    }

    @Test
    public void testWriteBuffered() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(10);

        int written = sut_.write(buffer);

        assertThat(written, is(10));
        assertThat(buffer.remaining(), is(0));
        verify(baseChannel_, never()).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testWriteAutoFlush() throws Exception {
        final byte[] data0 = new byte[20];
        Arrays.fill(data0, (byte) '0');
        ByteBuffer buffer0 = ByteBuffer.wrap(data0);
        final byte[] data1 = new byte[21];
        Arrays.fill(data1, (byte) '1');
        ByteBuffer buffer1 = ByteBuffer.wrap(data1);
        when(baseChannel_.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
                ByteBuffer expected = ByteBuffer.allocate(sut_.buffer_.capacity());
                expected.put(data0, 0, data0.length);
                expected.put(data1, 0, expected.capacity() - data0.length);
                expected.flip();
                assertThat(buffer, is(expected));
                return expected.capacity();
            }
        });

        int written0 = sut_.write(buffer0);
        int written1 = sut_.write(buffer1);

        assertThat(written0, is(20));
        assertThat(buffer0.remaining(), is(0));
        assertThat(written1, is(21));
        assertThat(buffer1.remaining(), is(0));
        verify(baseChannel_).write(Mockito.any(ByteBuffer.class));
        ByteBuffer expectedBuffer = ByteBuffer.allocate(sut_.buffer_.capacity());
        int p = sut_.buffer_.capacity() - data0.length;
        expectedBuffer.put(data1, p, data1.length - p);
        ByteBuffer actualBuffer = sut_.buffer_;

        // ByteBuffer.equals() assumes read-prepared buffer
        assertThat(actualBuffer.flip(), is(expectedBuffer.flip()));
    }

    @Test
    public void testWriteMultipleBuffers() throws Exception {
        ByteBuffer b0 = ByteBuffer.allocate(10);
        ByteBuffer b1 = ByteBuffer.allocate(10);
        ByteBuffer b2 = ByteBuffer.allocate(10);
        ByteBuffer[] buffers  = new ByteBuffer[]{b0, b1, b2};

        long written = sut_.write(buffers);

        assertThat(written, is(30L));
        assertThat(b0.remaining(), is(0));
        assertThat(b1.remaining(), is(0));
        assertThat(b2.remaining(), is(0));
        assertThat(sut_.buffer_.position(), is(30));
        verify(baseChannel_, never()).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testWriteMultipleBuffersAutoFlush() throws Exception {
        final byte[] data0 = new byte[20];
        Arrays.fill(data0, (byte) '0');
        ByteBuffer b0 = ByteBuffer.wrap(data0);
        final byte[] data1 = new byte[21];
        Arrays.fill(data1, (byte) '1');
        ByteBuffer b1 = ByteBuffer.wrap(data1);
        ByteBuffer[] buffers  = new ByteBuffer[]{b0, b1};
        when(baseChannel_.write(Mockito.any(ByteBuffer[].class), anyInt(), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer[] buffers = (ByteBuffer[]) invocation.getArguments()[0];
                long size = 0;
                for (ByteBuffer buffer : buffers) {
                    size += buffer.remaining();
                    buffer.position(buffer.limit()); // read all
                }
                return size;
            }
        });

        long written = sut_.write(buffers);

        verify(baseChannel_).write(Mockito.any(ByteBuffer[].class), anyInt(), anyInt());

        assertThat(written, is(41L));
        assertThat(b0.remaining(), is(0));
        assertThat(b1.remaining(), is(0));
        ByteBuffer actualBuffer = sut_.buffer_;
        assertThat(actualBuffer.remaining(), is(sut_.buffer_.capacity()));

    }

    @Test
    public void testFlush() throws Exception {
        final byte[] data = new byte[10];
        Arrays.fill(data, (byte) '0');
        ByteBuffer buffer = ByteBuffer.wrap(data);
        when(baseChannel_.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
                ByteBuffer expected = ByteBuffer.wrap(data);
                assertThat(buffer, is(expected));
                return 0;
            }
        });

        sut_.write(buffer);
        sut_.flush();

        verify(baseChannel_).write(Mockito.any(ByteBuffer.class));
    }
}
