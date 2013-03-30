package net.ihiroky.niotty.codec.string;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.CodecBuffer;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * Created on 13/01/18, 14:12
 *
 * @author Hiroki Itoh
 */
public class StringDecoder implements LoadStage<CodecBuffer, String> {

    private final CharsetDecoder decoder_;

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    public StringDecoder() {
        decoder_ = DEFAULT_CHARSET.newDecoder();
    }

    public StringDecoder(Charset charset) {
        decoder_ = charset.newDecoder();
    }

    @Override
    public void load(LoadStageContext<CodecBuffer, String> context, CodecBuffer input) {
        String s = input.readString(decoder_);
        context.proceed(s);
    }

    @Override
    public void load(LoadStageContext<CodecBuffer, String> context, TransportStateEvent event) {
        context.proceed(event);
    }
}
