package net.ihiroky.niotty.sample.drr;

import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.CodecBuffer;

/**
 * @author Hiroki Itoh
 */
public class EvenOddReporter extends LoadStage {

    private int evenCounter_;
    private int oddCounter_;

    @Override
    public void loaded(StageContext context, Object message) {
        CodecBuffer input = (CodecBuffer) message;
        int i = input.readInt();
        if (i % 2 == 0) {
            evenCounter_++;
        } else {
            oddCounter_++;
        }
        System.out.println(System.currentTimeMillis() + " "
                + Thread.currentThread().getName()
                + " a message is arrived; input:" + i + ", even:" + evenCounter_ + ", odd:" + oddCounter_);
    }

    @Override
    public void exceptionCaught(StageContext context, Exception exception) {
        exception.printStackTrace();
    }

    @Override
    public void activated(StageContext context) {
    }

    @Override
    public void deactivated(StageContext context, DeactivateState state) {
    }
}
