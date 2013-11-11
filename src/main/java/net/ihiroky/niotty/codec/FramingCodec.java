package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.Pipeline;
import net.ihiroky.niotty.Stage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;

/**
 * @author Hiroki Itoh
 */
public class FramingCodec implements Stage {

    private int poolingFrameBytes_;
    private CodecBuffer buffer_;

    static final int SHORT_BYTES = 2;
    static final int INT_FLAG = 0x80000000;
    static final int SHIFT_TWO_BYTES = 16;
    static final int MASK_TWO_BYTES = 0xFFFF;

    @Override
    public void stored(StageContext context, Object message) {
        BufferSink input = (BufferSink) message;
        int contentsLength = input.remaining();
        CodecBuffer headerBuffer;
        if (contentsLength <= Short.MAX_VALUE) {
            headerBuffer = Buffers.newCodecBuffer(SHORT_BYTES);
            headerBuffer.writeShort((short) contentsLength);
        } else {
            headerBuffer = Buffers.newCodecBuffer(SHORT_BYTES + SHORT_BYTES);
            headerBuffer.writeInt(INT_FLAG | contentsLength);
        }
        input.addFirst(headerBuffer);
        context.proceed(input);
    }

    @Override
    public void loaded(StageContext context, Object message) {
        CodecBuffer input = (CodecBuffer) message;
        while (input.remaining() > 0) {
            int frameBytes = poolingFrameBytes_;

            // load frame length
            if (frameBytes == 0) {
                CodecBuffer b = readFully(input, SHORT_BYTES);
                if (b == null) {
                    break;
                }
                int length = b.readShort();
                if (length >= 0) { // it's also satisfies length <= Short.MAX_VALUE
                    frameBytes = length;
                } else {
                    length <<= SHIFT_TWO_BYTES;
                    b = readFully(input, SHORT_BYTES);
                    if (b == null) {
                        poolingFrameBytes_ = length; // negative
                        break;
                    }
                    int upper = length & ~INT_FLAG;
                    int lower = b.readShort() & MASK_TWO_BYTES;
                    frameBytes = upper | lower;
                }
            } else if (frameBytes < 0) {
                CodecBuffer b = readFully(input, SHORT_BYTES);
                if (b == null) {
                    break;
                }
                int upper = frameBytes & ~INT_FLAG;
                int lower = b.readShort() & MASK_TWO_BYTES;
                frameBytes = (upper << SHIFT_TWO_BYTES) | lower;
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
    public void exceptionCaught(StageContext context, Exception exception) {
    }

    @Override
    public void activated(StageContext context) {
    }

    @Override
    public void deactivated(StageContext context, Pipeline.DeactivateState state) {
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
