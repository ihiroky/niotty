package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.CodecBuffer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Created on 13/01/18, 14:12
 *
 * @author Hiroki Itoh
 */
public class StringDecoder implements LoadStage<CodecBuffer, String> {

    private final Charset charset_;

    public StringDecoder() {
        charset_ = StandardCharsets.UTF_8;
    }

    public StringDecoder(Charset charset) {
        Objects.requireNonNull(charset, "charset");
        charset_ = charset;
    }

    @Override
    public void load(StageContext<String> context, CodecBuffer input) {
        String s = input.readString(charset_.newDecoder(), input.remainingBytes());
        context.proceed(s);
    }

    @Override
    public void load(StageContext<String> context, TransportStateEvent event) {
        context.proceed(event);
    }
}
