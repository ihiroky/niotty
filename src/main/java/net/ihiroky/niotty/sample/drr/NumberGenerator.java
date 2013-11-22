package net.ihiroky.niotty.sample.drr;

import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.codec.WeightedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class NumberGenerator extends LoadStage {

    private Logger logger_ = LoggerFactory.getLogger(NumberGenerator.class);

    @Override
    public void loaded(StageContext context, Object message, Object parameter) {
        CodecBuffer input = (CodecBuffer) message;
        int count = input.readInt();
        Transport transport = context.transport();
        System.out.println("count:" + count);
        CodecBuffer[] buffers = new CodecBuffer[count];
        for (int i = 0; i < count; i++) {
            CodecBuffer buffer = Buffers.newCodecBuffer(1024);
            for (int j = 0; j < 256; j++) {
                buffer.writeInt(i);
            }
            buffers[i] = buffer;
        }
        for (int i = 0; i < count; i++) {
            transport.write(new WeightedMessage(buffers[i], -((i + 1) % 2)));
        }
    }

    @Override
    public void exceptionCaught(StageContext context, Exception exception) {
        logger_.error("[exceptionCaught]", exception);
    }

    @Override
    public void activated(StageContext context) {
        logger_.info("[activated]");
    }

    @Override
    public void deactivated(StageContext context, DeactivateState state) {
        logger_.info("[deactivated] state:{}", state);
    }
}
