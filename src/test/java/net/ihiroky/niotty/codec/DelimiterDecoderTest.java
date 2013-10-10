package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.buffer.ArrayCodecBuffer;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.Charsets;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.nio.charset.Charset;

import static net.ihiroky.niotty.codec.ReferenceCountMatcher.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

/**
 * @author Hiroki Itoh
 */
public class DelimiterDecoderTest {

    private static final Charset CHARSET = Charsets.UTF_8;

    private void assertContent(CodecBuffer buffer, Matcher<byte[]> matcher) {
        byte[] b = new byte[buffer.remainingBytes()];
        buffer.readBytes(b, 0, b.length);
        assertThat(b, matcher);
    }

    @Test
    public void testLoad_One() throws Exception {
        DelimiterDecoder sut = new DelimiterDecoder(new byte[]{'\r', '\n'}, false);
        StageContextMock<CodecBuffer> context = new StageContextMock<CodecBuffer>();

        byte[] data = "input\r\n".getBytes(CHARSET);
        CodecBuffer input = Buffers.wrap(data, 0, data.length);
        sut.load(context, input);

        assertContent(context.pollEvent(), is(data));
        assertThat(context.hasNoEvent(), is(true));
        assertThat(sut.buffer(), is(nullValue()));
        assertThat(input, hasReferenceCount(1)); // alive yet
    }

    @Test
    public void testLoad_Two() throws Exception {
        DelimiterDecoder sut = new DelimiterDecoder(new byte[]{'\r', '\n'}, false);
        StageContextMock<CodecBuffer> context = new StageContextMock<CodecBuffer>();

        byte[] data = "input0\r\ninput1\r\n".getBytes(CHARSET);
        CodecBuffer input = Buffers.wrap(data, 0, data.length);
        sut.load(context, input);

        assertContent(context.pollEvent(), is("input0\r\n".getBytes(CHARSET)));
        assertContent(context.pollEvent(), is("input1\r\n".getBytes(CHARSET)));
        assertThat(context.hasNoEvent(), is(true));
        assertThat(sut.buffer(), is(nullValue()));
        assertThat(input, hasReferenceCount(2)); // for input0 and input1
    }

    @Test
    public void testLoad_IncompletePacket0() throws Exception {
        DelimiterDecoder sut = new DelimiterDecoder(new byte[]{'\r', '\n'}, false);
        StageContextMock<CodecBuffer> context = new StageContextMock<CodecBuffer>();

        byte[] data = "input0\r".getBytes(CHARSET);
        CodecBuffer input = Buffers.wrap(data, 0, data.length);
        sut.load(context, input);

        assertThat(context.hasNoEvent(), is(true));
        assertContent(sut.buffer(), is(data));
        assertThat(((ArrayCodecBuffer) input).referenceCount(), is(0));
    }

    @Test
    public void testLoad_IncompletePacket1() throws Exception {
        DelimiterDecoder sut = new DelimiterDecoder(new byte[]{'\r', '\n'}, false);
        StageContextMock<CodecBuffer> context = new StageContextMock<CodecBuffer>();

        byte[] data = "input0\r\ninput1".getBytes(CHARSET);
        CodecBuffer input = Buffers.wrap(data, 0, data.length);
        sut.load(context, input);

        assertContent(context.pollEvent(), is("input0\r\n".getBytes(CHARSET)));
        assertThat(context.hasNoEvent(), is(true));
        assertThat(sut.buffer().beginning(), is(0));
        assertContent(sut.buffer(), is("input1".getBytes(CHARSET)));
        assertThat(((ArrayCodecBuffer) input).referenceCount(), is(1)); // for input0
    }

    @Test
    public void testLoad_IncompletePacketSeparate() throws Exception {
        DelimiterDecoder sut = new DelimiterDecoder(new byte[]{'\r', '\n'}, false);
        StageContextMock<CodecBuffer> context = new StageContextMock<CodecBuffer>();

        byte[] data0 = "input0\r".getBytes(CHARSET);
        byte[] data1 = "\ninput1".getBytes(CHARSET);
        CodecBuffer input0 = Buffers.wrap(data0, 0, data0.length);
        CodecBuffer input1 = Buffers.wrap(data1, 0, data1.length);
        sut.load(context, input0);
        sut.load(context, input1);

        assertContent(context.pollEvent(), is("input0\r\n".getBytes(CHARSET)));
        assertThat(context.hasNoEvent(), is(true));
        assertThat(sut.buffer().beginning(), is(0));
        assertContent(sut.buffer(), is("input1".getBytes(CHARSET)));
        assertThat(((ArrayCodecBuffer) input0).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) input1).referenceCount(), is(0));
    }

    @Test
    public void testLoad_IncompletePacketSeparateTwice() throws Exception {
        DelimiterDecoder sut = new DelimiterDecoder(new byte[]{'\r', '\n'}, false);
        StageContextMock<CodecBuffer> context = new StageContextMock<CodecBuffer>();

        byte[] data0 = "input0\r\ninput1\r".getBytes(CHARSET);
        byte[] data1 = "\ninput2\r".getBytes(CHARSET);
        CodecBuffer input0 = Buffers.wrap(data0, 0, data0.length);
        CodecBuffer input1 = Buffers.wrap(data1, 0, data1.length);
        sut.load(context, input0);
        sut.load(context, input1);

        assertContent(context.pollEvent(), is("input0\r\n".getBytes(CHARSET)));
        assertContent(context.pollEvent(), is("input1\r\n".getBytes(CHARSET)));
        assertThat(context.hasNoEvent(), is(true));
        assertThat(sut.buffer().beginning(), is(0));
        assertContent(sut.buffer(), is("input2\r".getBytes(CHARSET)));
        assertThat(((ArrayCodecBuffer) input0).referenceCount(), is(1)); // for input0
        assertThat(((ArrayCodecBuffer) input1).referenceCount(), is(0));
    }

    @Test
    public void testLoad_IncompletePacketSeparateAndComplete() throws Exception {
        DelimiterDecoder sut = new DelimiterDecoder(new byte[]{'\r', '\n'}, false);
        StageContextMock<CodecBuffer> context = new StageContextMock<CodecBuffer>();

        byte[] data0 = "input0\r\ninput1\r".getBytes(CHARSET);
        byte[] data1 = "\ninput2\r\n".getBytes(CHARSET);
        CodecBuffer input0 = Buffers.wrap(data0, 0, data0.length);
        CodecBuffer input1 = Buffers.wrap(data1, 0, data1.length);
        sut.load(context, input0);
        sut.load(context, input1);

        assertContent(context.pollEvent(), is("input0\r\n".getBytes(CHARSET)));
        assertContent(context.pollEvent(), is("input1\r\n".getBytes(CHARSET)));
        assertContent(context.pollEvent(), is("input2\r\n".getBytes(CHARSET)));
        assertThat(context.hasNoEvent(), is(true));
        assertThat(sut.buffer(), is(nullValue()));
        assertThat(((ArrayCodecBuffer) input0).referenceCount(), is(1)); // for input0
        assertThat(((ArrayCodecBuffer) input1).referenceCount(), is(0));
    }

    @Test
    public void testLoad_RemoveDelimiter() throws Exception {
        DelimiterDecoder sut = new DelimiterDecoder(new byte[]{'\r', '\n'}, true);
        StageContextMock<CodecBuffer> context = new StageContextMock<CodecBuffer>();

        byte[] data0 = "input0\r\ninput1\r".getBytes(CHARSET);
        byte[] data1 = "\ninput2\r\n".getBytes(CHARSET);
        CodecBuffer input0 = Buffers.wrap(data0, 0, data0.length);
        CodecBuffer input1 = Buffers.wrap(data1, 0, data1.length);
        sut.load(context, input0);
        sut.load(context, input1);

        assertContent(context.pollEvent(), is("input0".getBytes(CHARSET)));
        assertContent(context.pollEvent(), is("input1".getBytes(CHARSET)));
        assertContent(context.pollEvent(), is("input2".getBytes(CHARSET)));
        assertThat(context.hasNoEvent(), is(true));
        assertThat(sut.buffer(), is(nullValue()));
        assertThat(((ArrayCodecBuffer) input0).referenceCount(), is(1)); // for input0
        assertThat(((ArrayCodecBuffer) input1).referenceCount(), is(0));
    }
}
