package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.TransportStateEvent;

/**
 * @author Hiroki Itoh
 */
public class FrameLengthRemoveDecoder implements LoadStage<CodecBuffer, CodecBuffer> {

    private int poolingFrameBytes_;
    private CodecBuffer pooling_;

    @Override
    public void load(LoadStageContext<CodecBuffer, CodecBuffer> context, CodecBuffer input) {
        while (input.remainingBytes() > 0) {
            int frameBytes = poolingFrameBytes_;

            // load frame length
            if (frameBytes == 0) {
                if (input.remainingBytes() < FrameLengthPrependEncoder.SHORT_BYTES) {
                    return;
                }
                int length = input.readShort() & FrameLengthPrependEncoder.MASK_TWO_BYTES;
                if (length >= 0) { // length < Short.MAX_VALUE
                    frameBytes = length;
                } else {
                    if (input.remainingBytes() < FrameLengthPrependEncoder.SHORT_BYTES) {
                        poolingFrameBytes_ = length; // negative
                        return;
                    }
                    int upper = length & ~FrameLengthPrependEncoder.INT_FLAG;
                    int lower = input.readShort() & FrameLengthPrependEncoder.MASK_TWO_BYTES;
                    frameBytes = (upper << FrameLengthPrependEncoder.SHIFT_TWO_BYTES) | lower;
                }
            } else if (frameBytes < 0) {
                if (input.remainingBytes() < FrameLengthPrependEncoder.SHORT_BYTES) {
                    return;
                }
                int upper = poolingFrameBytes_ & ~FrameLengthPrependEncoder.INT_FLAG;
                int lower = input.readShort() & FrameLengthPrependEncoder.MASK_TWO_BYTES;
                frameBytes = (upper << FrameLengthPrependEncoder.SHIFT_TWO_BYTES) | lower;
            }

            // load frame
            CodecBuffer output = readFully(input, frameBytes);
            if (output == null) {
                poolingFrameBytes_ = frameBytes;
                return;
            }

            poolingFrameBytes_ = 0;
            context.proceed(output);
        }
    }

    @Override
    public void load(LoadStageContext<?, ?> context, TransportStateEvent event) {
        context.proceed(event);
    }

    private CodecBuffer readFully(CodecBuffer input, int requiredLength) {
        if (pooling_ != null) {
            pooling_.drainFrom(input, requiredLength - pooling_.remainingBytes());
            if (pooling_.remainingBytes() == requiredLength) {
                CodecBuffer fulfilled = pooling_;
                pooling_ = null;
                return fulfilled;
            }
            return null;
        }

        int remainingBytes = input.remainingBytes();
        if (remainingBytes >= requiredLength) {
            CodecBuffer p = Buffers.wrap(new byte[requiredLength], 0, 0);
            p.drainFrom(input, requiredLength);
            return p;
        }
        if (remainingBytes == 0) {
            return null;
        }
        CodecBuffer p = Buffers.wrap(new byte[requiredLength], 0, 0);
        p.drainFrom(input, requiredLength);
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
