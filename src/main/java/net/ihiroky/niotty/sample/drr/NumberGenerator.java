package net.ihiroky.niotty.sample.drr;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hiroki Itoh
 */
public class NumberGenerator implements LoadStage<CodecBuffer, Void> {

    private Logger logger_ = LoggerFactory.getLogger(NumberGenerator.class);

    @Override
    public void load(LoadStageContext<CodecBuffer, Void> context, CodecBuffer input) {
        int count = input.readInt();
        Transport transport = context.transport();
        System.out.println("count:" + count);
        CodecBuffer[] buffers = new CodecBuffer[count];
        for (int i = 0; i < count; i++) {
            CodecBuffer buffer = Buffers.newPriorityCodecBuffer(1024, -((i + 1) % 2));
            for (int j = 0; j < 256; j++) {
                buffer.writeInt(i);
            }
            buffers[i] = buffer;
        }
        for (int i = 0; i < count; i++) {
            transport.write(buffers[i]);
        }
//        for (int i = 0; i < count / 2; i++) {
//            transport.write(buffers[i]);
//        }
//        try { Thread.sleep(100); }
//        catch (Exception e) { e.printStackTrace(); }
//        for (int i = count / 2; i < count; i++) {
//            transport.write(buffers[i]);
//        }
    }

    @Override
    public void load(LoadStageContext<CodecBuffer, Void> context, TransportStateEvent event) {
        logger_.info("state: {}", event);
    }
}
