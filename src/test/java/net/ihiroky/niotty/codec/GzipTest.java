package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.DefaultTransportStateEvent;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.JavaVersion;
import net.ihiroky.niotty.util.Platform;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;

import static net.ihiroky.niotty.util.JavaVersionMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.Assume.*;

/**
 * @author Hiroki Itoh
 */
public class GzipTest {

    private StageContextMock<CodecBuffer> context_;

    @Rule
    public TemporaryFolder tmpFileRule_ = new TemporaryFolder();

    @Before
    public void setUp() {
        assumeThat(Platform.javaVersion(), greaterOrEqual(JavaVersion.JAVA7));
        context_ = new StageContextMock<CodecBuffer>();
    }

    @Test
    public void testEncodeAndDecode_Simple() throws Exception {
        byte[] data = "0123456789012345678901234567890123456789012345678901234567890123456789".getBytes("UTF-8");
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        GzipEncoder encoder = new GzipEncoder();
        encoder.store(context_, buffer);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        GzipDecoder decoder = new GzipDecoder();
        decoder.load(context_, deflated);

        CodecBuffer inflated = context_.pollEvent();
        assertThat(inflated.remaining(), is(data.length));
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
    public void testEncodeAndDecode_OutputIsMultiple() throws Exception {
        byte[] data = new byte[512];
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        GzipEncoder encoder = new GzipEncoder();
        encoder.store(context_, buffer);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        GzipDecoder decoder = new GzipDecoder();
        decoder.load(context_, deflated);

        assertProcessedData(data);
    }

    // no dictionary support

    @Test
    public void testEncodeAndDecode_NeedsInput() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        GzipEncoder encoder = new GzipEncoder();
        encoder.store(context_, buffer);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        GzipDecoder decoder = new GzipDecoder();
        decoder.load(context_, deflated.slice(8));
        decoder.load(context_, deflated);

        assertProcessedData(data);
    }

    @Test
    public void testEncodeAndDecode_EncodeBufferIsSmall() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);
        GzipEncoder encoder = new GzipEncoder(Deflater.BEST_SPEED, 8);
        encoder.store(context_, buffer);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        GzipDecoder decoder = new GzipDecoder();
        decoder.load(context_, deflated);

        assertProcessedData(data);
    }

    @Test
    public void testEncodeAndDecode_InflaterBufferIsSmall() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        GzipEncoder encoder = new GzipEncoder();
        encoder.store(context_, buffer);
        CodecBuffer deflated = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        GzipDecoder decoder = new GzipDecoder(8);
        decoder.load(context_, deflated);

        assertProcessedData(data);
    }

    @Test
    public void testEncodeAndDecode_Finished() throws Exception {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        GzipEncoder encoder = new GzipEncoder();
        encoder.store(context_, buffer);
        encoder.store(context_, new DefaultTransportStateEvent(TransportState.CLOSED, null));
        CodecBuffer deflated = context_.pollEvent();
        CodecBuffer finished = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }
        GzipDecoder decoder = new GzipDecoder();
        decoder.load(context_, deflated);
        decoder.load(context_, finished);

        assertProcessedData(data);
    }

    @Test
    public void testEncode_DecodedByCommand() throws Exception {
        final String command = "/bin/gzip";
        assumeThat(new File(command).exists(), is(true));

        byte[] data = new byte[512];
        Arrays.fill(data, (byte) '0');
        CodecBuffer buffer = Buffers.wrap(data, 0, data.length);

        GzipEncoder encoder = new GzipEncoder();
        encoder.store(context_, buffer);
        encoder.store(context_, new DefaultTransportStateEvent(TransportState.CLOSED, null));
        CodecBuffer deflated = context_.pollEvent();
        CodecBuffer finished = context_.pollEvent();
        if (context_.pollEvent() != null) {
            Assert.fail();
        }

        File file = tmpFileRule_.newFile();
        FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
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
        Files.write(file.toPath(), data);

        byte[] input = new byte[data.length];
        int inputLength;
        InputStream in = commandInputStream(Arrays.asList(command, "-c", file.getPath()));
        try {
            inputLength = in.read(input, 0, input.length);
        } finally {
            in.close();
        }

        GzipDecoder decoder = new GzipDecoder();
        CodecBuffer buffer = Buffers.wrap(input, 0, inputLength);
        decoder.load(context_, buffer);

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
