package net.ihiroky.niotty.sample.drr;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.CodecBuffer;

/**
 * @author Hiroki Itoh
 */
public class EvenOddReporter implements LoadStage<CodecBuffer, Void> {

    private int evenCounter_;
    private int oddCounter_;

    @Override
    public void load(StageContext<Void> context, CodecBuffer input) {
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
    public void load(StageContext<?> context, TransportStateEvent event) {
    }
}
