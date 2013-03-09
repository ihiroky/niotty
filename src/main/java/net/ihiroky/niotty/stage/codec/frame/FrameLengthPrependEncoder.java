package net.ihiroky.niotty.stage.codec.frame;

import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.StoreStageContext;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.buffer.CodecBufferDeque;
import net.ihiroky.niotty.TransportStateEvent;

/**
 * @author Hiroki Itoh
 */
public class FrameLengthPrependEncoder implements StoreStage<CodecBufferDeque, CodecBufferDeque> {

    private static final int INITIAL_BUFFER_SIZE = 5;
    static final int MINIMUM_WHOLE_LENGTH = 5;

    @Override
    public void store(StoreStageContext<CodecBufferDeque, CodecBufferDeque> context, CodecBufferDeque input) {
        int contentsLength = input.remainingBytes();
        CodecBuffer headerBuffer = Buffers.newCodecBuffer(INITIAL_BUFFER_SIZE);
        headerBuffer.writeVariableByteInteger(contentsLength);

        int wholeLength = headerBuffer.remainingBytes() + contentsLength;
        if (wholeLength < MINIMUM_WHOLE_LENGTH) {
            appendTrailer(input, wholeLength);
        }

        input.addFirst(headerBuffer);
        context.proceed(input);
    }

    @Override
    public void store(StoreStageContext<CodecBufferDeque, CodecBufferDeque> context, TransportStateEvent event) {
        context.proceed(event);
    }

    private void appendTrailer(CodecBufferDeque contents, int wholeLength) {
        int trailerLength = MINIMUM_WHOLE_LENGTH - wholeLength;
        CodecBuffer tail = contents.peekLast();
        for (int i = 0; i < trailerLength; i++) {
            tail.writeByte(0);
        }
    }
}
