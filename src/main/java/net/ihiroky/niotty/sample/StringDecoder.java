package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.CodecBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * Created on 13/01/18, 14:12
 *
 * @author Hiroki Itoh
 */
public class StringDecoder implements LoadStage<CodecBuffer, String> {

    private Logger logger_ = LoggerFactory.getLogger(StringDecoder.class);

    private static final Charset CHARSET = Charset.forName("UTF-8");

    @Override
    public void load(LoadStageContext<CodecBuffer, String> context, CodecBuffer input) {
        String s = input.readString(CHARSET.newDecoder());
        context.proceed(s);
    }

    @Override
    public void load(LoadStageContext<CodecBuffer, String> context, TransportStateEvent event) {
        logger_.info(event.toString());
        context.proceed(event);
    }
}
