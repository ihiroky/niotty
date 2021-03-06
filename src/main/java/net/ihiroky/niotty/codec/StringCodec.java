package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.Stage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.Arguments;
import net.ihiroky.niotty.util.Charsets;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * Created on 13/01/18, 14:12
 *
 */
public class StringCodec implements Stage {

    private final CharsetEncoder encoder_;
    private final CharsetDecoder decoder_;
    private final byte[] trailer_;

    private static final byte[] NO_TRAILER = new byte[0];

    public StringCodec() {
        this(Charsets.UTF_8, NO_TRAILER);
    }

    public StringCodec(Charset charset) {
        this(charset, NO_TRAILER);
    }

    public StringCodec(Charset charset, byte[] trailer) {
        Arguments.requireNonNull(charset, "charset");
        encoder_ = charset.newEncoder();
        decoder_ = charset.newDecoder();
        trailer_ = Arguments.requireNonNull(trailer, "trailer");
    }

    @Override
    public void stored(StageContext context, Object message, Object parameter) {
        String input = (String) message;
        encoder_.reset();
        float bytesPerChar = encoder_.averageBytesPerChar();
        int trailerLength = trailer_.length;
        CodecBuffer buffer = Buffers.newCodecBuffer(Math.round(bytesPerChar * input.length()) + trailerLength);
        buffer.writeStringContent(input, encoder_);
        buffer.writeBytes(trailer_, 0, trailerLength);
        context.proceed(buffer, parameter);
    }

    @Override
    public void loaded(StageContext context, Object message, Object parameter) {
        CodecBuffer input = (CodecBuffer) message;
        decoder_.reset();
        String s = input.readStringContent(decoder_, input.remaining());
        input.dispose();
        context.proceed(s, parameter);
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
}
