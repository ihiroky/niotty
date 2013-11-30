package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.buffer.Packet;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.Arguments;

import java.util.Arrays;

/**
 *
 */
public class DelimiterEncoder extends StoreStage {

    private byte[] delimiter_;

    public DelimiterEncoder(byte[] delimiter) {
        Arguments.requireNonNull(delimiter, "delimiter");
        if (delimiter.length == 0) {
            throw new IllegalArgumentException("The delimiter must not be empty.");
        }

        delimiter_ = Arrays.copyOf(delimiter, delimiter.length);
    }

    @Override
    public void stored(StageContext context, Object message, Object parameter) {
        Packet input = (Packet) message;
        CodecBuffer b = Buffers.wrap(delimiter_, 0, delimiter_.length);
        input.addLast(b);
        context.proceed(input, parameter);
    }

    @Override
    public void exceptionCaught(StageContext context, Exception exception) {
    }

    @Override
    public void activated(StageContext context) {
    }

    @Override
    public void deactivated(StageContext context, DeactivateState state) {
    }
}
