package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.DefaultTransportStateEvent;
import net.ihiroky.niotty.TransportState;
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
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        DeflaterEncoder encoder = new DeflaterEncoder();
        encoder.store(context_, buffer);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder();
        decoder.load(context_, deflated);

        CodecBuffer inflated = context_.pollEvent();
        assertThat(inflated.remainingBytes(), is(data.length));
        assertThat(decoder.output(), is(nullValue()));
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
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        DeflaterEncoder encoder = new DeflaterEncoder();
        encoder.store(context_, buffer);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder();
        decoder.load(context_, deflated);

        assertProcessedData(data);
    }

    @Test
    public void testDeflateAndInflate_Dictionary() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);
        byte[] dictionary = new byte[256];
        Arrays.fill(dictionary, (byte) '0');

        DeflaterEncoder encoder = new DeflaterEncoder(Deflater.BEST_SPEED, 1024, dictionary, false);
        encoder.store(context_, buffer);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder(1024, dictionary, false);
        decoder.load(context_, deflated);

        assertProcessedData(data);
    }

    @Test
    public void testDeflateAndInflate_NeedsInput() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        DeflaterEncoder encoder = new DeflaterEncoder();
        encoder.store(context_, buffer);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder();
        decoder.load(context_, deflated.slice(8));
        decoder.load(context_, deflated);

        assertProcessedData(data);
    }

    @Test
    public void testDeflateAndInflate_DeflaterBufferIsSmall() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);
        DeflaterEncoder encoder = new DeflaterEncoder(Deflater.BEST_SPEED, 8, null, false);
        encoder.store(context_, buffer);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder();
        decoder.load(context_, deflated);

        assertProcessedData(data);
    }

    @Test
    public void testDeflateAndInflate_InflaterBufferIsSmall() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        DeflaterEncoder encoder = new DeflaterEncoder();
        encoder.store(context_, buffer);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder(8, null, false);
        decoder.load(context_, deflated);

        assertProcessedData(data);
    }

    @Test
    public void testDeflateAndInflate_Finished() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        DeflaterEncoder encoder = new DeflaterEncoder();
        encoder.store(context_, buffer);
        encoder.store(context_, new DefaultTransportStateEvent(TransportState.CLOSED, null));
        CodecBuffer deflated = context_.pollEvent();
        CodecBuffer finished = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        InflateDecoder decoder = new InflateDecoder(8, null, false);
        decoder.load(context_, deflated);
        decoder.load(context_, finished);

        assertProcessedData(data);
    }
}
