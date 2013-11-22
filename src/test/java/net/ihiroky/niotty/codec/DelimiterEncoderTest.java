package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.util.Charsets;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

/**
 */
public class DelimiterEncoderTest {

    @Test
    public void testStore() throws Exception {
        DelimiterEncoder sut = new DelimiterEncoder(new byte[]{'\r', '\n'});
        StageContext context = new StageContextMock<BufferSink>();

        byte[] data = "input".getBytes(Charsets.UTF_8);
        BufferSink b = Buffers.wrap(data, 0, data.length);
        sut.stored(context, b, new Object());

        ByteBuffer buffer = ByteBuffer.allocate(16);
        b.copyTo(buffer);
        buffer.flip();

        byte[] expected = new byte[data.length + 2];
        buffer.get(expected, 0, data.length);
        expected[data.length] = '\r';
        expected[data.length + 1] = '\n';
        assertThat(Arrays.copyOf(buffer.array(), buffer.limit()), is(expected));
    }
}
