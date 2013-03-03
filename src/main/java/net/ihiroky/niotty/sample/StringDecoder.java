package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

/**
 * Created on 13/01/18, 14:12
 *
 * @author Hiroki Itoh
 */
public class StringDecoder implements LoadStage<CodecBuffer, String> {

    private Logger logger_ = LoggerFactory.getLogger(StringDecoder.class);

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private CharsetDecoder decoder_ = CHARSET.newDecoder()
            .onMalformedInput(CodingErrorAction.IGNORE)
            .onUnmappableCharacter(CodingErrorAction.IGNORE);

    @Override
    public void load(LoadStageContext<CodecBuffer, String> context, MessageEvent<CodecBuffer> event) {
        CodecBuffer message = event.getMessage();
        try {
            String s = decoder_.decode(message.toByteBuffer()).toString();
            context.proceed(new MessageEvent<>(event.getTransport(), s));
        } catch (CharacterCodingException cce) {
            cce.printStackTrace();
        }
    }

    @Override
    public void load(LoadStageContext<CodecBuffer, String> context, TransportStateEvent event) {
        logger_.info(event.toString());
        context.proceed(event);
    }
}
