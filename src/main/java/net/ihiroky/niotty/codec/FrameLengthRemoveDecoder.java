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
        while (input.remaining() > 0) {
            int frameBytes = poolingFrameBytes_;

            // load frame length
            if (frameBytes == 0) {
                CodecBuffer b = readFully(input, FrameLengthPrependEncoder.SHORT_BYTES);
                if (b == null) {
                    break;
                }
                int length = b.readShort();
                if (length >= 0) { // it's also satisfies length <= Short.MAX_VALUE
                    frameBytes = length;
                } else {
                    length <<= FrameLengthPrependEncoder.SHIFT_TWO_BYTES;
                    b = readFully(input, FrameLengthPrependEncoder.SHORT_BYTES);
                    if (b == null) {
                        poolingFrameBytes_ = length; // negative
                        break;
                    }
                    int upper = length & ~FrameLengthPrependEncoder.INT_FLAG;
                    int lower = b.readShort() & FrameLengthPrependEncoder.MASK_TWO_BYTES;
                    frameBytes = upper | lower;
                }
            } else if (frameBytes < 0) {
                CodecBuffer b = readFully(input, FrameLengthPrependEncoder.SHORT_BYTES);
                if (b == null) {
                    break;
                }
                int upper = frameBytes & ~FrameLengthPrependEncoder.INT_FLAG;
                int lower = b.readShort() & FrameLengthPrependEncoder.MASK_TWO_BYTES;
                frameBytes = (upper << FrameLengthPrependEncoder.SHIFT_TWO_BYTES) | lower;
            }

            // load frame
            CodecBuffer output = readFully(input, frameBytes);
            if (output == null) {
                poolingFrameBytes_ = frameBytes; // positive
                break;
            }
            if (output == input) {
                output = input.slice(frameBytes);
            }

            poolingFrameBytes_ = 0;
            context.proceed(output);
        }
        input.dispose();
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
     * @return the buffer which contains the data at least the {@code requiredLength}, or null.
     */
    CodecBuffer readFully(CodecBuffer input, int requiredLength) {
        if (buffer_ != null) {
            buffer_.drainFrom(input, requiredLength - buffer_.remaining());
            if (buffer_.remaining() == requiredLength) {
                CodecBuffer fulfilled = buffer_;
                buffer_ = null;
                return fulfilled;
            }
            return null;
        }

        int remainingBytes = input.remaining();
        if (remainingBytes >= requiredLength) {
            return input;
        }
        if (remainingBytes > 0) {
            buffer_ = Buffers.newCodecBuffer(input, requiredLength);
        }
        return null;
    }

    int getPoolingFrameBytes() {
        return poolingFrameBytes_;
    }

    CodecBuffer getPooling() {
        return buffer_;
    }
}
