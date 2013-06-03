package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StoreStageContextMock;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

/**
 * @author Hiroki Itoh
 */
public class DelimiterEncoderTest {

    @Test
    public void testStore() throws Exception {
        DelimiterEncoder sut = new DelimiterEncoder(new byte[]{'\r', '\n'});
        StageContext<BufferSink> context = new StoreStageContextMock<>(sut);

        byte[] data = "input".getBytes(StandardCharsets.UTF_8);
        BufferSink b = Buffers.wrap(data, 0, data.length);
        sut.store(context, b);

        ByteBuffer buffer = ByteBuffer.allocate(16);
        b.transferTo(buffer);
        buffer.flip();

        byte[] expected = new byte[data.length + 2];
        buffer.get(expected, 0, data.length);
        expected[data.length] = '\r';
        expected[data.length + 1] = '\n';
        assertThat(Arrays.copyOf(buffer.array(), buffer.limit()), is(expected));
    }
}
