package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.Charsets;
import net.ihiroky.niotty.util.Objects;

import java.nio.charset.Charset;

/**
 * Created on 13/01/18, 14:12
 *
 */
public class StringDecoder implements LoadStage<CodecBuffer, String> {

    private final Charset charset_;

    public StringDecoder() {
        charset_ = Charsets.UTF_8;
    }

    public StringDecoder(Charset charset) {
        Objects.requireNonNull(charset, "charset");
        charset_ = charset;
    }

    @Override
    public void load(StageContext<String> context, CodecBuffer input) {
        String s = input.readString(charset_.newDecoder(), input.remainingBytes());
        input.dispose();
        context.proceed(s);
    }

    @Override
    public void load(StageContext<String> context, TransportStateEvent event) {
    }
}
