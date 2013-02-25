package net.ihiroky.niotty.stage.codec.frame;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.DecodeBuffer;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * @author Hiroki Itoh
 */
public class LengthRemoveDecoder implements LoadStage<DecodeBuffer, DecodeBuffer> {

    private int frameBytes;
    private DecodeBuffer pooling;

    @Override
    public void load(LoadStageContext<DecodeBuffer, DecodeBuffer> context, MessageEvent<DecodeBuffer> event) {
        DecodeBuffer input = event.getMessage();

        int length = frameBytes;

        // load frame length
        if (length == 0) {
            input = fulfill(input, LengthPrependEncoder.MINIMUM_WHOLE_LENGTH);
            if (input == null) {
                return;
            }
            length = input.readVariableByteInteger();
        }

        // load frame
        input = fulfill(input, length);
        if (input == null) {
            frameBytes = length;
            return;
        }

        frameBytes = 0;
        context.proceed(new MessageEvent<>(event.getTransport(), input));
    }

    @Override
    public void load(LoadStageContext<DecodeBuffer, DecodeBuffer> context, TransportStateEvent event) {
        context.proceed(event);
    }

    private DecodeBuffer fulfill(DecodeBuffer input, int requiredLength) {
        if (pooling != null) {
            pooling.drainFrom(input);
            if (pooling.remainingBytes() >= requiredLength) {
                DecodeBuffer fulfilled = pooling;
                pooling = null;
                return fulfilled;
            }
            return null;
        }

        if (input.remainingBytes() >= requiredLength) {
            return input;
        }

        DecodeBuffer p = Buffers.newDecodeBuffer(new byte[requiredLength], 0, 0);
        p.drainFrom(input);
        pooling = p;
        return null;
    }
}
