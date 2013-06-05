package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;

/**
 * @author Hiroki Itoh
 */
public class FrameLengthRemoveDecoder implements LoadStage<CodecBuffer, CodecBuffer> {

    private int poolingFrameBytes_;
    private CodecBuffer buffer_;

    @Override
    public void load(StageContext<CodecBuffer> context, CodecBuffer input) {
        while (input.remainingBytes() > 0) {
            int frameBytes = poolingFrameBytes_;

            // load frame length
            if (frameBytes == 0) {
                CodecBuffer b = readFully(input, FrameLengthPrependEncoder.SHORT_BYTES, true);
                if (b == null) {
                    return;
                }
                int length = b.readShort();
                if (length >= 0) { // it's also satisfies length <= Short.MAX_VALUE
                    frameBytes = length;
                } else {
                    length <<= FrameLengthPrependEncoder.SHIFT_TWO_BYTES;
                    b = readFully(input, FrameLengthPrependEncoder.SHORT_BYTES, true);
                    if (b == null) {
                        poolingFrameBytes_ = length; // negative
                        return;
                    }
                    int upper = length & ~FrameLengthPrependEncoder.INT_FLAG;
                    int lower = b.readShort() & FrameLengthPrependEncoder.MASK_TWO_BYTES;
                    frameBytes = upper | lower;
                }
            } else if (frameBytes < 0) {
                CodecBuffer b = readFully(input, FrameLengthPrependEncoder.SHORT_BYTES, true);
                if (b == null) {
                    return;
                }
                int upper = frameBytes & ~FrameLengthPrependEncoder.INT_FLAG;
                int lower = b.readShort() & FrameLengthPrependEncoder.MASK_TWO_BYTES;
                frameBytes = (upper << FrameLengthPrependEncoder.SHIFT_TWO_BYTES) | lower;
            }

            // load frame
            CodecBuffer output = readFully(input, frameBytes, false);
            if (output == null) {
                poolingFrameBytes_ = frameBytes; // positive
                return;
            }

            poolingFrameBytes_ = 0;
            context.proceed(output);
        }
    }

    @Override
    public void load(StageContext<CodecBuffer> context, TransportStateEvent event) {
    }

    /**
     * <p>Reads data to the amount of specified {@code requiredLength}.</p>
     *
     * <p>If enough data exist in the {@code input}, then this method returns a buffer
     * which contains data at least {@code requiredLength}. Otherwise, returns null
     * and pools a content of the {@code input} to a member field.</p>
     *
     * @param input a input buffer
     * @param requiredLength expected read length
     * @param noCopyIfEnough true if this method returns the {@code input} when the {@code input}
     *                       has enough data to read data to the amount of {@code requiredLength},
     *                       or returns new copied buffer.
     * @return the buffer which contains the data at least the {@code requiredLength}, or null.
     */
    CodecBuffer readFully(CodecBuffer input, int requiredLength, boolean noCopyIfEnough) {
        if (buffer_ != null) {
            buffer_.drainFrom(input, requiredLength - buffer_.remainingBytes());
            if (buffer_.remainingBytes() == requiredLength) {
                CodecBuffer fulfilled = buffer_;
                buffer_ = null;
                return fulfilled;
            }
            return null;
        }

        int remainingBytes = input.remainingBytes();
        if (remainingBytes >= requiredLength) {
            return noCopyIfEnough ? input : copy(input, requiredLength);
        }
        if (remainingBytes == 0) {
            return null;
        }
        buffer_ = copy(input, requiredLength);
        return null;
    }

    private static CodecBuffer copy(CodecBuffer input, int bytes) {
        CodecBuffer b = Buffers.wrap(new byte[bytes], 0, 0);
        b.drainFrom(input, bytes);
        return b;
    }

    int getPoolingFrameBytes() {
        return poolingFrameBytes_;
    }

    CodecBuffer getPooling() {
        return buffer_;
    }
}
