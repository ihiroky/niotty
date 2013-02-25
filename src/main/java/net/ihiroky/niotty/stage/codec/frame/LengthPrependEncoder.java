package net.ihiroky.niotty.stage.codec.frame;

import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.StoreStageContext;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.EncodeBuffer;
import net.ihiroky.niotty.buffer.EncodeBufferGroup;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * @author Hiroki Itoh
 */
public class LengthPrependEncoder implements StoreStage<EncodeBufferGroup, EncodeBufferGroup> {

    private static final int INITIAL_BUFFER_SIZE = 5;
    static final int MINIMUM_WHOLE_LENGTH = 5;

    @Override
    public void store(StoreStageContext<EncodeBufferGroup, EncodeBufferGroup> context,
                        MessageEvent<EncodeBufferGroup> event) {
        EncodeBufferGroup contents = event.getMessage();
        int contentsLength = contents.filledBytes();
        EncodeBuffer headerBuffer = Buffers.newEncodeBuffer(INITIAL_BUFFER_SIZE);
        headerBuffer.writeVariableByteInteger(contentsLength);

        int wholeLength = headerBuffer.filledBytes() + contentsLength;
        if (wholeLength < MINIMUM_WHOLE_LENGTH) {
            appendTrailer(contents, wholeLength);
        }

        contents.addFirst(headerBuffer);
        context.proceed(event);
    }

    @Override
    public void store(StoreStageContext<EncodeBufferGroup, EncodeBufferGroup> context, TransportStateEvent event) {
        context.proceed(event);
    }

    private void appendTrailer(EncodeBufferGroup contents, int wholeLength) {
        int trailerLength = MINIMUM_WHOLE_LENGTH - wholeLength;
        EncodeBuffer tail = contents.peekLast();
        for (int i = 0; i < trailerLength; i++) {
            tail.writeByte(0);
        }
    }
}
