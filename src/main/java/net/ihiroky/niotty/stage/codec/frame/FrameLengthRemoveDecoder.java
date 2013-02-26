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

    private int poolingFrameBytes_;
    private DecodeBuffer pooling_;

    @Override
    public void load(LoadStageContext<DecodeBuffer, DecodeBuffer> context, MessageEvent<DecodeBuffer> event) {
        DecodeBuffer input = event.getMessage();

        for (;;) {
            int frameBytes = poolingFrameBytes_;

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
                poolingFrameBytes_ = frameBytes;
                return;
            }

            poolingFrameBytes_ = 0;
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
        if (pooling_ != null) {
            pooling_.drainFrom(input);
            if (pooling_.remainingBytes() >= requiredLength) {
                DecodeBuffer fulfilled = pooling_;
                pooling_ = null;
                return fulfilled;
            }
            return null;
        }

        if (input.remainingBytes() >= requiredLength) {
            return input;
        }

        DecodeBuffer p = Buffers.newDecodeBuffer(new byte[requiredLength], 0, 0);
        p.drainFrom(input);
        pooling_ = p;
        return null;
    }

    int getPoolingFrameBytes() {
        return poolingFrameBytes_;
    }

    DecodeBuffer getPooling() {
        return pooling_;
    }
}
