package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.StoreStageContext;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.EncodeBuffer;
import net.ihiroky.niotty.buffer.EncodeBufferGroup;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * Created on 13/01/18, 12:59
 *
 * @author Hiroki Itoh
 */
public class StringEncoder implements StoreStage<String, EncodeBufferGroup> {

    private Logger logger = LoggerFactory.getLogger(StringEncoder.class);

    static Charset CHARSET = Charset.forName("UTF-8");

    @Override
    public void store(StoreStageContext<String, EncodeBufferGroup> context, MessageEvent<String> event) {
        String message = event.getMessage();
        EncodeBuffer buffer = Buffers.newEncodeBuffer();
        buffer.writeString(CHARSET.newEncoder(), message);
        EncodeBufferGroup group = new EncodeBufferGroup().addFirst(buffer);
        context.proceed(new MessageEvent<>(event.getTransport(), group));
    }

    @Override
    public void store(StoreStageContext<String, EncodeBufferGroup> context, TransportStateEvent event) {
        logger.info(event.toString());
        context.proceed(event);
    }
}
