package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.buffer.ArrayCodecBuffer;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.Assume.*;

/**
 * @author Hiroki Itoh
 */
public class JZlibTest {

    private StageContextMock<CodecBuffer> context_;

    @Rule
    public TemporaryFolder tmpFileRule_ = new TemporaryFolder();

    @Before
    public void setUp() {
        context_ = new StageContextMock<CodecBuffer>();
    }

    @Test
    public void testDeflateAndInflate_Simple() throws Exception {
        byte[] data = "0123456789012345678901234567890123456789012345678901234567890123456789".getBytes("UTF-8");
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);

        JZlibDeflaterEncoder encoder = new JZlibDeflaterEncoder();
        encoder.stored(context_, deflaterInput, null);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder();
        decoder.loaded(context_, inflaterInput, null);

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
        assertThat("content", decompressed, is(data));
        assertThat("length", n, is(data.length));
    }

    @Test
    public void testDeflateAndInflate_OutputIsMultiple() throws Exception {
        byte[] data = new byte[512];
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);

        JZlibDeflaterEncoder encoder = new JZlibDeflaterEncoder();
        encoder.stored(context_, deflaterInput, null);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder();
        decoder.loaded(context_, inflaterInput, null);

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

        JZlibDeflaterEncoder encoder =
                new JZlibDeflaterEncoder(JZlibDeflaterEncoder.BEST_SPEED, 1024, dictionary, false);
        encoder.stored(context_, deflaterInput, null);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder(1024, dictionary, false);
        decoder.loaded(context_, inflaterInput, null);

        assertProcessedData(data);
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput).referenceCount(), is(0));
    }

    @Test
    public void testDeflateAndInflate_NeedsInput() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);

        JZlibDeflaterEncoder encoder = new JZlibDeflaterEncoder();
        encoder.stored(context_, deflaterInput, null);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder();
        decoder.loaded(context_, inflaterInput.slice(8), null);
        decoder.loaded(context_, inflaterInput, null);

        assertProcessedData(data);
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput).referenceCount(), is(0));
    }

    @Test
    public void testDeflateAndInflate_DeflaterBufferIsSmall() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);
        JZlibDeflaterEncoder encoder = new JZlibDeflaterEncoder(JZlibDeflaterEncoder.BEST_SPEED, 8, null, false);
        encoder.stored(context_, deflaterInput, null);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder();
        decoder.loaded(context_, inflaterInput, null);

        assertProcessedData(data);
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput).referenceCount(), is(0));
    }

    @Test
    public void testDeflateAndInflate_InflaterBufferIsSmall() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);

        JZlibDeflaterEncoder encoder = new JZlibDeflaterEncoder();
        encoder.stored(context_, deflaterInput, null);
        CodecBuffer inflaterInput = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder(8, null, false);
        decoder.loaded(context_, inflaterInput, null);

        assertProcessedData(data);
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput).referenceCount(), is(0));
    }

    @Test
    public void testDeflateAndInflate_Finished() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer deflaterInput = Buffers.wrap(data, 0, data.length);

        JZlibDeflaterEncoder encoder = new JZlibDeflaterEncoder();
        encoder.stored(context_, deflaterInput, null);
        encoder.deactivated(context_, DeactivateState.WHOLE);
        CodecBuffer inflaterInput0 = context_.pollEvent();
        CodecBuffer inflaterInput1 = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder(8, null, false);
        decoder.loaded(context_, inflaterInput0, null);
        decoder.loaded(context_, inflaterInput1, null);

        assertProcessedData(data);
        assertThat(((ArrayCodecBuffer) deflaterInput).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput0).referenceCount(), is(0));
        assertThat(((ArrayCodecBuffer) inflaterInput1).referenceCount(), is(0));
    }

    @Test
    public void testEncodeAndDecode_Simple() throws Exception {
        byte[] data = "0123456789012345678901234567890123456789012345678901234567890123456789".getBytes("UTF-8");
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        JZlibDeflaterEncoder encoder = new JZlibDeflaterEncoder(JZlibDeflaterEncoder.BEST_SPEED, true);
        encoder.stored(context_, buffer, null);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder(true);
        decoder.loaded(context_, deflated, null);

        CodecBuffer inflated = context_.pollEvent();
        assertThat(inflated.remaining(), is(data.length));
        assertThat(decoder.output(), is(nullValue()));
    }

    @Test
    public void testEncodeAndDecode_OutputIsMultiple() throws Exception {
        byte[] data = new byte[512];
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        JZlibDeflaterEncoder encoder = new JZlibDeflaterEncoder(JZlibDeflaterEncoder.BEST_SPEED, true);
        encoder.stored(context_, buffer, null);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder(true);
        decoder.loaded(context_, deflated, null);

        assertProcessedData(data);
    }

    // no dictionary support

    @Test
    public void testEncodeAndDecode_NeedsInput() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        JZlibDeflaterEncoder encoder = new JZlibDeflaterEncoder(JZlibDeflaterEncoder.BEST_SPEED, true);
        encoder.stored(context_, buffer, null);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder(true);
        decoder.loaded(context_, deflated.slice(8), null);
        decoder.loaded(context_, deflated, null);

        assertProcessedData(data);
    }

    @Test
    public void testEncodeAndDecode_EncodeBufferIsSmall() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);
        JZlibDeflaterEncoder encoder =
                new JZlibDeflaterEncoder(JZlibDeflaterEncoder.BEST_SPEED, 8, null, true);
        encoder.stored(context_, buffer, null);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder(true);
        decoder.loaded(context_, deflated, null);

        assertProcessedData(data);
    }

    @Test
    public void testEncodeAndDecode_InflaterBufferIsSmall() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        JZlibDeflaterEncoder encoder = new JZlibDeflaterEncoder(JZlibDeflaterEncoder.BEST_SPEED, true);
        encoder.stored(context_, buffer, null);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder(true);
        decoder.loaded(context_, deflated, null);

        assertProcessedData(data);
    }

    @Test
    public void testEncodeAndDecode_Finished() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        JZlibDeflaterEncoder encoder = new JZlibDeflaterEncoder(JZlibDeflaterEncoder.BEST_SPEED, true);
        encoder.stored(context_, buffer, null);
        encoder.deactivated(context_, DeactivateState.WHOLE);
        CodecBuffer deflated = context_.pollEvent();
        CodecBuffer finished = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder(true);
        decoder.loaded(context_, deflated, null);
        decoder.loaded(context_, finished, null);

        assertProcessedData(data);
    }

    @Test
    public void testEncode_DecodedByCommand() throws Exception {
        final String command = "/bin/gzip";
        assumeThat(new File(command).exists(), is(true));

        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        JZlibDeflaterEncoder encoder = new JZlibDeflaterEncoder(JZlibDeflaterEncoder.BEST_SPEED, true);
        encoder.stored(context_, buffer, null);
        encoder.deactivated(context_, DeactivateState.WHOLE);
        CodecBuffer deflated = context_.pollEvent();
        CodecBuffer finished = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }

        File file = tmpFileRule_.newFile();
        FileChannel fileChannel = new RandomAccessFile(file, "rw").getChannel();
        try {
            fileChannel.write(deflated.byteBuffer());
            fileChannel.write(finished.byteBuffer());
        } finally {
            fileChannel.close();
        }

        byte[] input = new byte[data.length];
        InputStream in = commandInputStream(Arrays.asList(command, "-cd", file.getPath()));
        try {
            in.read(input, 0, input.length);
        } finally {
            in.close();
        }
        assertThat(input, is(data));
    }

    @Test
    public void testDecode_EncodedByCommand() throws Exception {
        final String command = "/bin/gzip";
        assumeThat(new File(command).exists(), is(true));

        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');

        File file = tmpFileRule_.newFile();
        FileChannel fileChannel = new RandomAccessFile(file, "rw").getChannel();
        try {
            fileChannel.write(ByteBuffer.wrap(data));
        } finally {
            fileChannel.close();
        }

        byte[] input = new byte[data.length];
        int inputLength;
        InputStream in = commandInputStream(Arrays.asList(command, "-c", file.getPath()));
        try {
            inputLength = in.read(input, 0, input.length);
        } finally {
            in.close();
        }

        JZlibInflaterDecoder decoder = new JZlibInflaterDecoder(true);
        CodecBuffer buffer = Buffers.wrap(input, 0, inputLength);
        decoder.loaded(context_, buffer, null);

        assertProcessedData(data);
    }

    private InputStream commandInputStream(List<String> cmdLine) throws Exception {
        File cmd = new File(cmdLine.get(0));
        assumeThat(cmd.exists(), is(true));

        ProcessBuilder builder = new ProcessBuilder(cmdLine);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        process.waitFor();
        return process.getInputStream();
    }

}
