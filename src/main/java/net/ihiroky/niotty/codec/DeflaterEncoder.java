package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.JavaVersion;
import net.ihiroky.niotty.util.Platform;

import java.io.IOException;
import java.util.zip.Deflater;

/**
 * @author Hiroki Itoh
 */
public class DeflaterEncoder extends StoreStage {

    private final Deflater deflater_;
    private final BufferChannel buffer_;

    protected static final int DEFAULT_BUFFER_SIZE = 8192;

    @Override
    public void activated(StageContext context) {
    }

    public DeflaterEncoder() {
        this(Deflater.BEST_SPEED, DEFAULT_BUFFER_SIZE, null, false);
    }

    public DeflaterEncoder(int level) {
        this(level, DEFAULT_BUFFER_SIZE, null, false);
    }

    public DeflaterEncoder(int level, int bufferSize, byte[] dictionary, boolean nowrap) {

        Platform.javaVersion().throwIfUnsupported(JavaVersion.JAVA7);

        deflater_ = new Deflater(level, nowrap);
        buffer_ = new BufferChannel(new byte[bufferSize]);
        if (dictionary != null) {
            deflater_.setDictionary(dictionary);
        }
    }

    @Override
    public void stored(StageContext context, Object message, Object parameter) {
        BufferSink input = (BufferSink) message;
        CodecBuffer output = input.hasArray() ? compressRawArray(input) : compressBufferSink(input);
        input.dispose();
        context.proceed(output, parameter);
    }

    @Override
    public void exceptionCaught(StageContext context, Exception exception) {
    }

    private CodecBuffer compressRawArray(BufferSink input) {
        CodecBuffer output = Buffers.newCodecBuffer((int) (input.remaining() * 0.7f + 10));

        byte[] inputBytes = input.array();
        int inputOffset = input.arrayOffset();
        int inputLength = input.remaining();

        onBeforeEncode(inputBytes, inputOffset, inputLength, output);

        deflater_.setInput(inputBytes, inputOffset, inputLength);
        byte[] outputBytes = output.array();
        int outputOffset = output.endIndex();
        int outputLength = outputBytes.length;
        for (;;) {
            int n = deflater_.deflate(outputBytes, outputOffset, outputLength - outputOffset, Deflater.SYNC_FLUSH);
            outputOffset += n;
            if (deflater_.needsInput()) {
                output.endIndex(outputOffset);
                return output;
            }
            if (outputOffset == outputLength) {
                CodecBuffer newOutput = Buffers.newCodecBuffer(outputLength);
                newOutput.writeBytes(outputBytes, 0, outputOffset);
                outputBytes = newOutput.array();
                outputLength = outputBytes.length;
            }
        }
    }

    private CodecBuffer compressBufferSink(BufferSink input) {
        CodecBuffer output = Buffers.newCodecBuffer((int) (input.remaining() * 0.7f + 10));

        try {
            while (input.remaining() > 0) {
                buffer_.reset();
                input.sink(buffer_);
                byte[] inputBytes = buffer_.array();
                int inputLength = buffer_.position();
                onBeforeEncode(inputBytes, 0, inputLength, output);

                deflater_.setInput(inputBytes, 0, inputLength);
                byte[] outputBytes = output.array();
                int outputOffset = output.endIndex();
                int outputLength = outputBytes.length;
                for (;;) {
                    int n = deflater_.deflate(outputBytes,
                            outputOffset, outputLength - outputOffset, Deflater.SYNC_FLUSH);
                    outputOffset += n;
                    if (deflater_.needsInput()) {
                        output.endIndex(outputOffset);
                        return output;
                    }
                    if (outputOffset == outputLength) {
                        CodecBuffer newOutput = Buffers.newCodecBuffer(outputLength);
                        newOutput.writeBytes(outputBytes, 0, outputOffset);
                        outputBytes = newOutput.array();
                        outputLength = outputBytes.length;
                    }
                }
            }
            return output;
        } catch (IOException ignored) {
            throw new AssertionError(ignored);
        }
    }

    @Override
    public void deactivated(StageContext context, DeactivateState state) {
        if (!deflater_.finished()
                && (state == DeactivateState.STORE || state == DeactivateState.WHOLE)) {
            CodecBuffer output = Buffers.newCodecBuffer(16);
            deflater_.finish();
            while (!deflater_.finished()) {
                buffer_.reset();
                byte[] outputBytes = buffer_.array();
                int n = deflater_.deflate(outputBytes, 0, outputBytes.length, Deflater.NO_FLUSH);
                output.writeBytes(outputBytes, 0, n);
            }

            onAfterFinished(output, deflater_);

            deflater_.end();
            if (output.remaining() > 0) {
                context.proceed(output, null); // may be error on non-connected udp
            }
        }
    }

    protected void onBeforeEncode(byte[] input, int offset, int length, CodecBuffer output) {
    }

    protected void onAfterFinished(CodecBuffer footer, Deflater deflater) {
    }
}
