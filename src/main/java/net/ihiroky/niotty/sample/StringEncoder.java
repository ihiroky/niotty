package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Stage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.EncodeBuffer;
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
public class StringEncoder implements Stage<String, BufferSink> {

    private Logger logger = LoggerFactory.getLogger(StringEncoder.class);

    static Charset CHARSET = Charset.forName("UTF-8");

    @Override
    public void process(StageContext<String, BufferSink> context, MessageEvent<String> event) {
        String message = event.getMessage();
        //byte[] bytes = message.getBytes(CHARSET);
        //context.proceed(new MessageEvent<>(event.getTransport(), Buffers.createBufferSink(bytes)));
        EncodeBuffer buffer = Buffers.newEncodeBuffer();
        buffer.writeString(CHARSET.newEncoder(), message);
        context.proceed(new MessageEvent<>(event.getTransport(), buffer.createBufferSink()));
    }

    @Override
    public void process(StageContext<String, BufferSink> context, TransportStateEvent event) {
        logger.info(event.toString());
        context.proceed(event);
    }
}
