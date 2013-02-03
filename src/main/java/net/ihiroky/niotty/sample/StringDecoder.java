package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Stage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.DecodeBuffer;
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
public class StringDecoder implements Stage<DecodeBuffer> {

    private Logger logger = LoggerFactory.getLogger(StringDecoder.class);

    private static Charset CHARSET = Charset.forName("UTF-8");

    private CharsetDecoder decoder = CHARSET.newDecoder()
            .onMalformedInput(CodingErrorAction.IGNORE)
            .onUnmappableCharacter(CodingErrorAction.IGNORE);

    @Override
    public void process(StageContext context, MessageEvent<DecodeBuffer> event) {
        DecodeBuffer message = event.getMessage();
        try {
            String s = decoder.decode(message.toByteBuffer()).toString();
            context.proceed(new MessageEvent<>(event.getTransport(), s));
        } catch (CharacterCodingException cce) {
            cce.printStackTrace();
        }
    }

    @Override
    public void process(StageContext context, TransportStateEvent event) {
        logger.info(event.toString());
        context.proceed(event);
    }
}
