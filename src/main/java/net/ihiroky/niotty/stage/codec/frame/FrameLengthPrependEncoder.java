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
    static final int MINIMUM_WHOLE_LENGTH = 5;

    @Override
    public void store(StoreStageContext<BufferSink, BufferSink> context, BufferSink input) {
        int contentsLength = input.remainingBytes();
        CodecBuffer headerBuffer = Buffers.newCodecBuffer(INITIAL_BUFFER_SIZE);
        headerBuffer.writeVariableByteInteger(contentsLength);

        BufferSink output = input;
        int wholeLength = headerBuffer.remainingBytes() + contentsLength;
        if (wholeLength < MINIMUM_WHOLE_LENGTH) {
            output = appendTrailer(output, wholeLength);
        }

        output = Buffers.newBufferSink(headerBuffer, output);
        context.proceed(output);
    }

    @Override
    public void store(StoreStageContext<BufferSink, BufferSink> context, TransportStateEvent event) {
        context.proceed(event);
    }

    private BufferSink appendTrailer(BufferSink contents, int wholeLength) {
        int trailerLength = MINIMUM_WHOLE_LENGTH - wholeLength;
        CodecBuffer trailer = Buffers.newCodecBuffer(trailerLength);
        for (int i = 0; i < trailerLength; i++) {
            trailer.writeByte(0);
        }
        return Buffers.newBufferSink(contents, trailer);
    }
}
