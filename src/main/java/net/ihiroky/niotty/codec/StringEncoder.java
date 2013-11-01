package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.Arguments;
import net.ihiroky.niotty.util.Charsets;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * Created on 13/01/18, 12:59
 *
 */
public class StringEncoder implements StoreStage<String, BufferSink> {

    private final Charset charset_;

    public StringEncoder() {
        charset_ = Charsets.UTF_8;
    }

    public StringEncoder(Charset charset) {
        Arguments.requireNonNull(charset, "charset");
        charset_ = charset;
    }

    @Override
    public void store(StageContext<BufferSink> context, String input) {
        CharsetEncoder encoder = charset_.newEncoder();
        float bytesPerChar = encoder.averageBytesPerChar();
        CodecBuffer buffer = Buffers.newCodecBuffer(Math.round(bytesPerChar * input.length()));
        buffer.writeString(input, encoder);
        context.proceed(buffer);
    }

    @Override
    public void store(StageContext<BufferSink> context, TransportStateEvent event) {
    }
}