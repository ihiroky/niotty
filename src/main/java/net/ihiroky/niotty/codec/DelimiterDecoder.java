package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class DelimiterDecoder implements LoadStage<CodecBuffer, CodecBuffer> {

    private final byte[] delimiter_;
    private final boolean removeDelimiter_;
    private CodecBuffer buffer_;

    public DelimiterDecoder(byte[] delimiter, boolean removeDelimiter) {
        Objects.requireNonNull(delimiter, "delimiter");
        if (delimiter.length == 0) {
            throw new IllegalArgumentException("The delimiter must not be empty.");
        }

        delimiter_ = Arrays.copyOf(delimiter, delimiter.length);
        removeDelimiter_ = removeDelimiter;
    }

    @Override
    public void load(StageContext<CodecBuffer> context, CodecBuffer input) {
        int end;
        for (;;) {
            CodecBuffer b = bufferOfInput(input);
            end = b.indexOf(delimiter_, 0);
            if (end == -1) {
                if (buffer_ != null) {
                    if (buffer_.remainingBytes() > 0) {
                        buffer_.compact(); // The remaining is processed later.
                    } else {
                        buffer_ = null;
                    }
                    return;
                }
                int remaining = b.remainingBytes();
                if (remaining > 0) {
                    buffer_ = Buffers.newCodecBuffer(remaining);
                    buffer_.drainFrom(input);
                }
                return;
            }

            int frameLength = removeDelimiter_ ? end : end + delimiter_.length;
            CodecBuffer output = Buffers.newCodecBuffer(frameLength);
            output.drainFrom(b, frameLength);
            if (removeDelimiter_) {
                b.skipBytes(delimiter_.length);
            }
            context.proceed(output);
        }
    }

    @Override
    public void load(StageContext<?> context, TransportStateEvent event) {
        context.proceed(event);
    }

    private CodecBuffer bufferOfInput(CodecBuffer input) {
        if (buffer_ == null) {
            return input;
        }
        buffer_.drainFrom(input);
        return buffer_;
    }

    CodecBuffer buffer() {
        return buffer_;
    }
}
