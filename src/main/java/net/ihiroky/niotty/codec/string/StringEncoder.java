package net.ihiroky.niotty.codec.string;

import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.StoreStageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * Created on 13/01/18, 12:59
 *
 * @author Hiroki Itoh
 */
public class StringEncoder implements StoreStage<String, BufferSink> {

    private final CharsetEncoder encoder_;

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    public StringEncoder() {
        encoder_ = DEFAULT_CHARSET.newEncoder();
    }

    public StringEncoder(Charset charset) {
        encoder_ = charset.newEncoder();
    }

    @Override
    public void store(StoreStageContext<String, BufferSink> context, String input) {
        CodecBuffer buffer = Buffers.newCodecBuffer();
        buffer.writeString(encoder_, input);
        context.proceed(buffer);
    }

    @Override
    public void store(StoreStageContext<String, BufferSink> context, TransportStateEvent event) {
        context.proceed(event);
    }
}
