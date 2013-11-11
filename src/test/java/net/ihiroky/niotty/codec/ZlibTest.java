package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.buffer.ArrayCodecBuffer;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.JavaVersion;
import net.ihiroky.niotty.util.Platform;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.zip.Deflater;

import static net.ihiroky.niotty.util.JavaVersionMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.Assume.*;

/**
 * @author Hiroki Itoh
 */
public class ZlibTest {

    private StageContextMock<CodecBuffer> context_;

    @Before
    public void setUp() {
        assumeThat(Platform.javaVersion(), greaterOrEqual(JavaVersion.JAVA7));
        context_ = new StageContextMock<CodecBuffer>();
    }

    @Test
    public void testDeflateAndInflate_Simple() throws Exception {
        byte[] data = "0123456789012345678901234567890123456789012345678901234567890123456789".getBytes("UTF-8");
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);

        DeflaterEncoder encoder = new DeflaterEncoder();
        encoder.stored(context_, deflaterInput);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflaterDecoder decoder = new InflaterDecoder();
        decoder.loaded(context_, inflaterInput);

        CodecBuffer inflated = context_.pollEvent();
        assertThat(inflated.remaining(), is(data.length));
        assertThat(decoder.output(), is(nullValue()));
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput).referenceCount(), is(0));
    }

    private void assertProcessedData(byte[] data) {
        byte[] decompressed = new byte[data.length];
        int n = 0;
        for (;;) {
            CodecBuffer b = context_.pollEvent();
            if (b == null) {
                break;
            }
            n += b.readBytes(decompressed, n, decompressed.length - n);
        }
        assertThat(decompressed, is(data));
        assertThat(n, is(data.length));
    }

    @Test
    public void testDeflateAndInflate_OutputIsMultiple() throws Exception {
        byte[] data = new byte[512];
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);

        DeflaterEncoder encoder = new DeflaterEncoder();
        encoder.stored(context_, deflaterInput);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflaterDecoder decoder = new InflaterDecoder();
        decoder.loaded(context_, inflaterInput);

        assertProcessedData(data);
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput).referenceCount(), is(0));
    }

    @Test
    public void testDeflateAndInflate_Dictionary() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);
        byte[] dictionary = new byte[256];
        Arrays.fill(dictionary, (byte) '0');

        DeflaterEncoder encoder = new DeflaterEncoder(Deflater.BEST_SPEED, 1024, dictionary, false);
        encoder.stored(context_, deflaterInput);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflaterDecoder decoder = new InflaterDecoder(1024, dictionary, false);
        decoder.loaded(context_, inflaterInput);

        assertProcessedData(data);
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput).referenceCount(), is(0));
    }

    @Test
    public void testDeflateAndInflate_NeedsInput() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);

        DeflaterEncoder encoder = new DeflaterEncoder();
        encoder.stored(context_, deflaterInput);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflaterDecoder decoder = new InflaterDecoder();
        decoder.loaded(context_, inflaterInput.slice(8));
        decoder.loaded(context_, inflaterInput);

        assertProcessedData(data);
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput).referenceCount(), is(0));
    }

    @Test
    public void testDeflateAndInflate_DeflaterBufferIsSmall() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);
        DeflaterEncoder encoder = new DeflaterEncoder(Deflater.BEST_SPEED, 8, null, false);
        encoder.stored(context_, deflaterInput);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflaterDecoder decoder = new InflaterDecoder();
        decoder.loaded(context_, inflaterInput);

        assertProcessedData(data);
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput).referenceCount(), is(0));
    }

    @Test
    public void testDeflateAndInflate_InflaterBufferIsSmall() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);

        DeflaterEncoder encoder = new DeflaterEncoder();
        encoder.stored(context_, deflaterInput);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflaterDecoder decoder = new InflaterDecoder(8, null, false);
        decoder.loaded(context_, inflaterInput);

        assertProcessedData(data);
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput).referenceCount(), is(0));
    }

    @Test
    public void testDeflateAndInflate_Finished() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);

        DeflaterEncoder encoder = new DeflaterEncoder();
        encoder.stored(context_, deflaterInput);
        encoder.deactivated(context_, DeactivateState.WHOLE);
        CodecBuffer inflaterInput0 = context_.pollEvent();
        CodecBuffer inflaterInput1 = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflaterDecoder decoder = new InflaterDecoder(8, null, false);
        decoder.loaded(context_, inflaterInput0);
        decoder.loaded(context_, inflaterInput1);

        assertProcessedData(data);
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput0).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput1).referenceCount(), is(0));
    }
}
