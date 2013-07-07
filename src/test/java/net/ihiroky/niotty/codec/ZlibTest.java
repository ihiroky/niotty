package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.DefaultTransportStateEvent;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.buffer.ArrayCodecBuffer;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.zip.Deflater;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

/**
 * @author Hiroki Itoh
 */
public class ZlibTest {

    private StageContextMock<CodecBuffer> context_;

    @Before
    public void setUp() {
        context_ = new StageContextMock<>();
    }

    @Test
    public void testDeflateAndInflate_Simple() throws Exception {
        byte[] data = "0123456789012345678901234567890123456789012345678901234567890123456789".getBytes("UTF-8");
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);

        DeflaterEncoder encoder = new DeflaterEncoder();
        encoder.store(context_, deflaterInput);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder();
        decoder.load(context_, inflaterInput);

        CodecBuffer inflated = context_.pollEvent();
        assertThat(inflated.remainingBytes(), is(data.length));
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
        encoder.store(context_, deflaterInput);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder();
        decoder.load(context_, inflaterInput);

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
        encoder.store(context_, deflaterInput);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder(1024, dictionary, false);
        decoder.load(context_, inflaterInput);

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
        encoder.store(context_, deflaterInput);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder();
        decoder.load(context_, inflaterInput.slice(8));
        decoder.load(context_, inflaterInput);

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
        encoder.store(context_, deflaterInput);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder();
        decoder.load(context_, inflaterInput);

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
        encoder.store(context_, deflaterInput);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder(8, null, false);
        decoder.load(context_, inflaterInput);

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
        encoder.store(context_, deflaterInput);
        encoder.store(context_, new DefaultTransportStateEvent(TransportState.CLOSED, null));
        CodecBuffer inflaterInput0 = context_.pollEvent();
        CodecBuffer inflaterInput1 = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder(8, null, false);
        decoder.load(context_, inflaterInput0);
        decoder.load(context_, inflaterInput1);

        assertProcessedData(data);
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput0).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput1).referenceCount(), is(0));
    }
}
