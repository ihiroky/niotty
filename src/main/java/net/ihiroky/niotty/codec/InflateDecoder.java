package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * @author Hiroki Itoh
 */
public class InflateDecoder implements LoadStage<CodecBuffer, CodecBuffer> {

    private Inflater inflater_;
    private byte[] buffer_;
    private byte[] dictionary_;
    private byte[] output_;

    protected static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int MAX_OUTPUT_BUFFER_SIZE = 8192;
    private static final int OUTPUT_SCALE = 4;
    private static final int INITIAL_SCALED_SIZE_LIMIT = MAX_OUTPUT_BUFFER_SIZE / OUTPUT_SCALE;
    private static final int MIN_OUTPUT_BUFFER_SIZE = 128;

    public InflateDecoder() {
        this(DEFAULT_BUFFER_SIZE, null, false);
    }

    public InflateDecoder(int bufferSize, byte[] dictionary, boolean nowrap) {
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
    public void load(StageContext<CodecBuffer> context, CodecBuffer input) {
        byte[] output = (output_ == null) ? new byte[outputBufferSize(input.remainingBytes())] : output_;
        Inflater inflater = inflater_;
        while (input.remainingBytes() > 0) {
            // TODO use internal byte array if input.hasArray() is true
            int n = input.readBytes(buffer_, 0, buffer_.length);
            try {
                int offset = onBeforeDecode(buffer_, 0, n);
                int remainingBytes = n - offset;
                if (remainingBytes == 0 // read all bytes
                        || remainingBytes > n) { // under reading header (offset == -1)
                    continue;
                }
                inflater.setInput(buffer_, offset, remainingBytes);
                for (;;) {
                    int decompressed = inflater.inflate(output, 0, output.length);
                    if (decompressed > 0) {
                        onAfterDecode(output, 0, decompressed);
                        CodecBuffer b = Buffers.wrap(output, 0, decompressed);
                        context.proceed(b);
                        int newRemaining = inflater.getRemaining();
                        if (newRemaining == 0) { // equals inflater.needsInput()
                            output_ = null;
                            break;
                        }
                        int processed = remainingBytes - newRemaining;
                        output = new byte[outputBufferSize(remainingBytes, decompressed, processed)];
                        remainingBytes = newRemaining;
                        continue;
                    }
                    if (inflater.finished()) {
                        output_ = null;
                        input.skipBytes(-inflater.getRemaining());
                        onAfterFinished(input, inflater);
                        break;
                    }
                    if (inflater.needsDictionary()) {
                        if (dictionary_ == null) {
                            throw new RuntimeException("A dictionary is required.");
                        }
                        inflater.setDictionary(dictionary_);
                    }
                }
            } catch (DataFormatException dfe) {
                throw new RuntimeException("Data format error.", dfe);
            }
        }
        input.dispose();
    }

    @Override
    public void load(StageContext<CodecBuffer> context, TransportStateEvent event) {
        if (event.state() == TransportState.CLOSED) {
            inflater_.end();
            output_ = null;
        }
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
