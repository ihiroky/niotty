package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.StoreStageContext;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.CodecBufferDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * Created on 13/01/18, 12:59
 *
 * @author Hiroki Itoh
 */
public class StringEncoder implements StoreStage<String, CodecBufferDeque> {

    private Logger logger_ = LoggerFactory.getLogger(StringEncoder.class);

    private static final Charset CHARSET = Charset.forName("UTF-8");

    @Override
    public void store(StoreStageContext<String, CodecBufferDeque> context, String input) {
        CodecBuffer buffer = Buffers.newCodecBuffer();
        buffer.writeString(CHARSET.newEncoder(), input);
        context.proceed(new CodecBufferDeque().addLast(buffer));
    }

    @Override
    public void store(StoreStageContext<String, CodecBufferDeque> context, TransportStateEvent event) {
        logger_.info(event.toString());
        context.proceed(event);
    }
}
