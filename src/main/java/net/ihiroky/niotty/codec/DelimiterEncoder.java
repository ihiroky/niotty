package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class DelimiterEncoder implements StoreStage<BufferSink, BufferSink> {

    private byte[] delimiter_;

    public DelimiterEncoder(byte[] delimiter) {
        Objects.requireNonNull(delimiter, "delimiter");
        if (delimiter.length == 0) {
            throw new IllegalArgumentException("The delimiter must not be empty.");
        }

        delimiter_ = Arrays.copyOf(delimiter, delimiter.length);
    }
    @Override
    public void store(StageContext<BufferSink> context, BufferSink input) {
        CodecBuffer b = Buffers.wrap(delimiter_, 0, delimiter_.length);
        input.addLast(b);
        context.proceed(input);
    }

    @Override
    public void store(StageContext<BufferSink> context, TransportStateEvent event) {
        context.proceed(event);
    }
}
