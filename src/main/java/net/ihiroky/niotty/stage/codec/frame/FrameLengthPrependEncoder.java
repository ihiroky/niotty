package net.ihiroky.niotty.stage.codec.frame;

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

    private static final int INITIAL_BUFFER_SIZE = 5;

    @Override
    public void store(StoreStageContext<BufferSink, BufferSink> context, BufferSink input) {
        int contentsLength = input.remainingBytes();
        CodecBuffer headerBuffer = Buffers.newCodecBuffer(INITIAL_BUFFER_SIZE);
        headerBuffer.writeVariableByteInteger(contentsLength);
        BufferSink output = Buffers.newBufferSink(headerBuffer, input);
        context.proceed(output);
    }

    @Override
    public void store(StoreStageContext<BufferSink, BufferSink> context, TransportStateEvent event) {
        context.proceed(event);
    }
}
