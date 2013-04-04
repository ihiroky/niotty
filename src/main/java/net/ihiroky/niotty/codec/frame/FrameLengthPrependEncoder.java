package net.ihiroky.niotty.codec.frame;

import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.StoreStageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;

/**
 * @author Hiroki Itoh
 */
public class FrameLengthPrependEncoder implements StoreStage<BufferSink, BufferSink> {

    static final int SHORT_BYTES = 2;
    static final int INT_FLAG = 0x80000000;
    static final int SHIFT_TWO_BYTES = 16;
    static final int MASK_TWO_BYTES = 0xFFFF;

    @Override
    public void store(StoreStageContext<BufferSink, BufferSink> context, BufferSink input) {
        int contentsLength = input.remainingBytes();
        if (contentsLength < 0) {
            throw new IllegalArgumentException("input length is negative: " + contentsLength);
        }
        CodecBuffer headerBuffer;
        if (contentsLength <= Short.MAX_VALUE) {
            headerBuffer = Buffers.newCodecBuffer(SHORT_BYTES);
            headerBuffer.writeShort((short) contentsLength);
        } else {
            headerBuffer = Buffers.newCodecBuffer(SHORT_BYTES + SHORT_BYTES);
            headerBuffer.writeInt(INT_FLAG | contentsLength);
        }
        BufferSink output = Buffers.newBufferSink(headerBuffer, input, input.priority());
        context.proceed(output);
    }

    @Override
    public void store(StoreStageContext<BufferSink, BufferSink> context, TransportStateEvent event) {
        context.proceed(event);
    }
}
