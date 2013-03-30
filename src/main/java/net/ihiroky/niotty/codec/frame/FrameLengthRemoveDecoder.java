package net.ihiroky.niotty.codec.frame;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.TransportStateEvent;

/**
 * @author Hiroki Itoh
 */
public class FrameLengthRemoveDecoder implements LoadStage<CodecBuffer, CodecBuffer> {

    private final boolean useSlice_;
    private int poolingFrameBytes_;
    private CodecBuffer pooling_;

    /** Max length of 'frame length'; variable byte of int. */
    private static final int MAX_FRAME_BYTE_LENGTH = 5;

    /** End bit of variable byte. */
    private static final int END_BIT = 0x80;

    private static final int MARGIN = 16;

    public FrameLengthRemoveDecoder() {
        useSlice_ = false;
    }

    public FrameLengthRemoveDecoder(boolean useSlice) {
        this.useSlice_ = useSlice;
    }

    @Override
    public void load(LoadStageContext<CodecBuffer, CodecBuffer> context, CodecBuffer input) {
        for (;;) {
            int frameBytes = poolingFrameBytes_;

            // load frame length
            if (frameBytes == 0) {
                input = readFrameLength(input);
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
                context.proceed(output);
                return;
            }
            // if (output.remainingBytes() > frameBytes) {
            CodecBuffer frame = useSlice_ ? output.slice(frameBytes) : copy(output, frameBytes);
            context.proceed(frame);
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

    private CodecBuffer readFrameLength(CodecBuffer input) {
        if (pooling_ != null) {
            pooling_.drainFrom(input);
            if (pooling_.remainingBytes() >= MAX_FRAME_BYTE_LENGTH) {
                CodecBuffer fulfilled = pooling_;
                pooling_ = null;
                return fulfilled;
            }
            return null;
        }

        int remainingBytes = input.remainingBytes();
        if (remainingBytes >= MAX_FRAME_BYTE_LENGTH || (input.readByte(remainingBytes - 1) & END_BIT) != 0) {
            return input;
        }

        if (remainingBytes > 0) {
            CodecBuffer p = Buffers.newCodecBuffer(new byte[MAX_FRAME_BYTE_LENGTH + MARGIN], 0, 0);
            p.drainFrom(input);
            pooling_ = p;
        }
        return null;
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

        int remainingBytes = input.remainingBytes();
        if (remainingBytes >= requiredLength) {
            return input;
        }

        if (remainingBytes > 0) {
            CodecBuffer p = Buffers.newCodecBuffer(new byte[requiredLength], 0, 0);
            p.drainFrom(input);
            pooling_ = p;
        }
        return null;
    }

    int getPoolingFrameBytes() {
        return poolingFrameBytes_;
    }

    CodecBuffer getPooling() {
        return pooling_;
    }
}
