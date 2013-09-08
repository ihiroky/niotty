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
        CodecBuffer b = bufferOfInput(input);
        for (;;) {
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
                int remaining = input.remainingBytes();
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
                b.skipBytes(delimiter_.length);
            }
            context.proceed(output);
        }
    }

    @Override
    public void load(StageContext<CodecBuffer> context, TransportStateEvent event) {
    }

    private CodecBuffer bufferOfInput(CodecBuffer input) {
        if (buffer_ == null) {
            return input;
        }
        buffer_.drainFrom(input);
        input.dispose();
        return buffer_;
    }

    CodecBuffer buffer() {
        return buffer_;
    }
}
