package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.Stage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.Packet;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.Arguments;

import java.util.Arrays;

/**
 *
 */
public class DelimiterFrameCodec implements Stage {

    private final byte[] delimiter_;
    private final boolean removeDelimiter_;
    private CodecBuffer buffer_;

    public DelimiterFrameCodec(byte[] delimiter, boolean removeDelimiter) {
        Arguments.requireNonNull(delimiter, "delimiter");
        if (delimiter.length == 0) {
            throw new IllegalArgumentException("The delimiter must not be empty.");
        }

        delimiter_ = Arrays.copyOf(delimiter, delimiter.length);
        removeDelimiter_ = removeDelimiter;
    }

    @Override
    public void stored(StageContext context, Object message, Object parameter) {
        Packet input = (Packet) message;
        CodecBuffer b = Buffers.wrap(delimiter_, 0, delimiter_.length);
        input.addLast(b);
        context.proceed(input, parameter);
    }

    @Override
    public void loaded(StageContext context, Object message, Object parameter) {
        CodecBuffer input = (CodecBuffer) message;
        int end;
        CodecBuffer b = bufferOfInput(input);
        for (;;) {
            end = b.indexOf(delimiter_, 0);
            if (end == -1) {
                if (buffer_ != null) {
                    if (buffer_.remaining() > 0) {
                        buffer_.compact(); // The remaining is processed later.
                    } else {
                        buffer_ = null;
                    }
                    return;
                }
                int remaining = input.remaining();
                if (remaining > 0) {
                    buffer_ = Buffers.newCodecBuffer(remaining);
                    buffer_.drainFrom(input);
                }
                input.dispose();
                return;
            }

            int frameLength = removeDelimiter_ ? end : end + delimiter_.length;
            CodecBuffer output = (b == input) ? b.slice(frameLength) : Buffers.newCodecBuffer(b, frameLength);
            if (removeDelimiter_) {
                b.skipStartIndex(delimiter_.length);
            }
            context.proceed(output, parameter);
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
    }

    @Override
    public void eventTriggered(StageContext context, Object event) {
    }

    private CodecBuffer bufferOfInput(CodecBuffer input) {
        if (buffer_ == null) {
            return input;
        }
        buffer_.drainFrom(input);
        input.dispose();
        return buffer_;
    }
}
