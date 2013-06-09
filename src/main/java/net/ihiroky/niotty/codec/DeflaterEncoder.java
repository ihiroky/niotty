package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;

import java.util.zip.Deflater;

/**
 * @author Hiroki Itoh
 */
public class DeflaterEncoder implements StoreStage<BufferSink, CodecBuffer> {

    private final Deflater deflater_;
    private final byte[] buffer_;

    protected static final int DEFAULT_BUFFER_SIZE = 8192;

    public DeflaterEncoder() {
        this(Deflater.BEST_SPEED, DEFAULT_BUFFER_SIZE, null, false);
    }

    public DeflaterEncoder(int level) {
        this(level, DEFAULT_BUFFER_SIZE, null, false);
    }

    public DeflaterEncoder(int level, int bufferSize, byte[] dictionary, boolean nowrap) {
        deflater_ = new Deflater(level, nowrap);
        buffer_ = new byte[bufferSize];
        if (dictionary != null) {
            deflater_.setDictionary(dictionary);
        }
    }

    @Override
    public void store(StageContext<CodecBuffer> context, BufferSink input) {
        CodecBuffer output = Buffers.newCodecBuffer((int) (input.remainingBytes() * 0.7f));

        byte[] inputBytes = input.array();
        int inputOffset = input.arrayOffset();
        int inputLength = input.remainingBytes();

        onBeforeEncode(inputBytes, inputOffset, inputLength, output);

        deflater_.setInput(inputBytes, inputOffset, inputLength);
        int length = buffer_.length;
        for (int n = length; n == length;) {
            n = deflater_.deflate(buffer_, 0, length, Deflater.SYNC_FLUSH);
            output.writeBytes(buffer_, 0, n);
        }
        input.dispose();
        context.proceed(output);
    }

    @Override
    public void store(StageContext<CodecBuffer> context, TransportStateEvent event) {
        if (event.state() == TransportState.CLOSED) {
            CodecBuffer output = Buffers.newCodecBuffer(16);
            deflater_.finish();
            while (!deflater_.finished()) {
                int n = deflater_.deflate(buffer_, 0, buffer_.length, Deflater.NO_FLUSH);
                output.writeBytes(buffer_, 0, n);
            }

            onAfterFinished(output, deflater_);

            deflater_.end();
            if (output.remainingBytes() > 0) {
                context.proceed(output);
            }
        }
    }

    protected void onBeforeEncode(byte[] input, int offset, int length, CodecBuffer output) {
    }

    protected void onAfterFinished(CodecBuffer footer, Deflater deflater) {
    }
}
