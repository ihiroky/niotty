package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.JavaVersion;
import net.ihiroky.niotty.util.Platform;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * @author Hiroki Itoh
 */
public class InflaterDecoder extends LoadStage {

    private Inflater inflater_;
    private byte[] buffer_;
    private byte[] dictionary_;
    private byte[] output_;
    private boolean deactivated_;

    protected static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int MAX_OUTPUT_BUFFER_SIZE = 8192;
    private static final int OUTPUT_SCALE = 4;
    private static final int INITIAL_SCALED_SIZE_LIMIT = MAX_OUTPUT_BUFFER_SIZE / OUTPUT_SCALE;
    private static final int MIN_OUTPUT_BUFFER_SIZE = 128;

    public InflaterDecoder() {
        this(DEFAULT_BUFFER_SIZE, null, false);
    }

    public InflaterDecoder(int bufferSize, byte[] dictionary, boolean nowrap) {

        Platform.javaVersion().throwIfUnsupported(JavaVersion.JAVA7);

        inflater_ = new Inflater(nowrap);
        buffer_ = new byte[bufferSize];
        dictionary_ = dictionary;
    }

    private static int outputBufferSize(int remaining) {
        return (remaining <= INITIAL_SCALED_SIZE_LIMIT) ? remaining * OUTPUT_SCALE : MAX_OUTPUT_BUFFER_SIZE;
    }

    private static int outputBufferSize(int remaining, int decompressed, int processed) {
        int next = (processed != 0)
                ? (int) (((float) remaining / processed) * decompressed) // processed / remaining = consumption rate
                : decompressed * OUTPUT_SCALE;
        return (next <= MAX_OUTPUT_BUFFER_SIZE) ? next : MIN_OUTPUT_BUFFER_SIZE;
    }

    @Override
    public void loaded(StageContext context, Object message, Object parameter) {
        CodecBuffer input = (CodecBuffer) message;
        byte[] buffer;
        int offset;
        int length;
        while (input.remaining() > 0) {
            if (input.hasArray()) {
                buffer = input.array();
                offset = input.arrayOffset() + input.startIndex();
                length = input.remaining();
                input.skipStartIndex(length);
            } else {
                buffer = buffer_;
                offset = 0;
                length = input.readBytes(buffer, 0, buffer.length);
            }
            try {
                if (inflate(context, buffer, offset, length, parameter)) {
                    input.skipStartIndex(inflater_.getRemaining());
                    onAfterFinished(input, inflater_);
                    break;
                }
            } catch (DataFormatException dfe) {
                throw new RuntimeException("Data format error.", dfe);
            }
        }
        input.dispose();
    }

    private boolean inflate(StageContext context,  byte[] buffer, int offset, int length, Object parameter)
            throws DataFormatException {

        Inflater inflater = inflater_;
        byte[] output = (output_ == null) ? new byte[outputBufferSize(length)] : output_;
        int n = onBeforeDecode(buffer, offset, length);
        int remainingBytes = length - n;
        if (remainingBytes == 0 // read all bytes
                || remainingBytes > length) { // under reading header (offset == -1)
            return false;
        }
        inflater.setInput(buffer, offset + n, remainingBytes);
        for (;;) {
            int decompressed = inflater.inflate(output, 0, output.length);
            if (decompressed > 0) {
                onAfterDecode(output, 0, decompressed);
                CodecBuffer b = Buffers.wrap(output, 0, decompressed);
                context.proceed(b, parameter);
                int newRemaining = inflater.getRemaining();
                if (newRemaining == 0) { // equals inflater.needsInput()
                    output_ = null;
                    return false;
                }
                int processed = remainingBytes - newRemaining;
                output = new byte[outputBufferSize(remainingBytes, decompressed, processed)];
                remainingBytes = newRemaining;
                continue;
            }
            if (inflater.finished()) {
                output_ = null;
                return true;
            }
            if (inflater.needsDictionary()) {
                if (dictionary_ == null) {
                    throw new RuntimeException("A dictionary is required.");
                }
                inflater.setDictionary(dictionary_);
            }
        }
    }

    @Override
    public void exceptionCaught(StageContext context, Exception exception) {
    }

    @Override
    public void activated(StageContext context) {
    }

    @Override
    public void deactivated(StageContext context) {
        if (!deactivated_) {
            inflater_.end();
            output_ = null;
            deactivated_ = true;
        }
    }

    @Override
    public void eventTriggered(StageContext context, Object event) {
    }

    protected int onBeforeDecode(byte[] output, int offset, int length) throws DataFormatException {
        return 0;
    }

    protected void onAfterDecode(byte[] output, int offset, int length) throws DataFormatException {
    }

    protected void onAfterFinished(CodecBuffer input, Inflater inflater) throws DataFormatException {
    }

    byte[] output() {
        return output_;
    }
}
