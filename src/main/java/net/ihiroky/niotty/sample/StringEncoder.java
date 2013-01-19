package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Stage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created on 13/01/18, 12:59
 *
 * @author Hiroki Itoh
 */
public class StringEncoder implements Stage<String> {

    private Logger logger = LoggerFactory.getLogger(StringEncoder.class);

    static Charset CHARSET = Charset.forName("UTF-8");

    @Override
    public void process(StageContext context, MessageEvent<String> event) {
        String message = event.getMessage();
        byte[] bytes = message.getBytes(CHARSET);
        context.proceed(new MessageEvent<ByteBuffer>(event.getTransport(), ByteBuffer.wrap(bytes)));
    }

    @Override
    public void process(StageContext context, TransportStateEvent event) {
        logger.info(event.toString());
        context.proceed(event);
    }
}
