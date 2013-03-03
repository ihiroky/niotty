package net.ihiroky.niotty.stage.codec.frame;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * @author Hiroki Itoh
 */
public class FrameLengthRemoveDecoder implements LoadStage<CodecBuffer, CodecBuffer> {

    private final boolean useSlice_;
    private int poolingFrameBytes_;
    private CodecBuffer pooling_;

    public FrameLengthRemoveDecoder() {
        useSlice_ = false;
    }

    public FrameLengthRemoveDecoder(boolean useSlice) {
        this.useSlice_ = useSlice;
    }

    @Override
    public void load(LoadStageContext<CodecBuffer, CodecBuffer> context, MessageEvent<CodecBuffer> event) {
        CodecBuffer input = event.getMessage();

        for (;;) {
            int frameBytes = poolingFrameBytes_;

            // load frame length
            if (frameBytes == 0) {
                input = readFully(input, FrameLengthPrependEncoder.MINIMUM_WHOLE_LENGTH);
                if (input == null) {
                    return;
                }
                frameBytes = input.readVariableByteInteger();
            }

            // load frame
            CodecBuffer output = readFully(input, frameBytes);
            if (output == null) {
                poolingFrameBytes_ = frameBytes;
                return;
            }

            poolingFrameBytes_ = 0;
            if (output.remainingBytes() == frameBytes) {
                context.proceed(new MessageEvent<>(event.getTransport(), output));
                return;
            }
            // if (output.remainingBytes() > frameBytes) {
            CodecBuffer frame = useSlice_ ? output.slice(frameBytes) : copy(output, frameBytes);
            context.proceed(new MessageEvent<>(event.getTransport(), frame));
        }
    }

    @Override
    public void load(LoadStageContext<CodecBuffer, CodecBuffer> context, TransportStateEvent event) {
        context.proceed(event);
    }

    private CodecBuffer copy(CodecBuffer output, int bytes) {
        CodecBuffer b = Buffers.newCodecBuffer(bytes);
        b.drainFrom(output, bytes);
        return b;
    }

    private CodecBuffer readFully(CodecBuffer input, int requiredLength) {
        if (pooling_ != null) {
            pooling_.drainFrom(input);
            if (pooling_.remainingBytes() >= requiredLength) {
                CodecBuffer fulfilled = pooling_;
                pooling_ = null;
                return fulfilled;
            }
            return null;
        }

        if (input.remainingBytes() >= requiredLength) {
            return input;
        }

        CodecBuffer p = Buffers.newCodecBuffer(new byte[requiredLength], 0, 0);
        p.drainFrom(input);
        pooling_ = p;
        return null;
    }

    int getPoolingFrameBytes() {
        return poolingFrameBytes_;
    }

    CodecBuffer getPooling() {
        return pooling_;
    }
}
