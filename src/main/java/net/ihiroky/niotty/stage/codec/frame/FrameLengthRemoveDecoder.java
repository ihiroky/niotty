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
public class FrameLengthRemoveDecoder implements LoadStage<DecodeBuffer, DecodeBuffer> {

    private int poolingFrameBytes;
    private DecodeBuffer pooling;

    @Override
    public void load(LoadStageContext<DecodeBuffer, DecodeBuffer> context, MessageEvent<DecodeBuffer> event) {
        DecodeBuffer input = event.getMessage();

        for (;;) {
            int frameBytes = poolingFrameBytes;

            // load frame length
            if (frameBytes == 0) {
                input = readFully(input, FrameLengthPrependEncoder.MINIMUM_WHOLE_LENGTH);
                if (input == null) {
                    return;
                }
                frameBytes = input.readVariableByteInteger();
            }

            // load frame
            DecodeBuffer output = readFully(input, frameBytes);
            if (output == null) {
                poolingFrameBytes = frameBytes;
                return;
            }

            poolingFrameBytes = 0;
            if (output.remainingBytes() == frameBytes) {
                context.proceed(new MessageEvent<>(event.getTransport(), output));
                return;
            }
            // if (output.remainingBytes() > frameBytes) {
            output = Buffers.newDecodeBuffer(frameBytes);
            output.drainFrom(input, frameBytes);
            context.proceed(new MessageEvent<>(event.getTransport(), output));
        }
    }

    @Override
    public void load(LoadStageContext<DecodeBuffer, DecodeBuffer> context, TransportStateEvent event) {
        context.proceed(event);
    }

    private DecodeBuffer readFully(DecodeBuffer input, int requiredLength) {
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

    int getPoolingFrameBytes() {
        return poolingFrameBytes;
    }

    DecodeBuffer getPooling() {
        return pooling;
    }
}
