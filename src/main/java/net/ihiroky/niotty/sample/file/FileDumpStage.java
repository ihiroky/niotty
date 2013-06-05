package net.ihiroky.niotty.sample.file;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.CodecBuffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * @author Hiroki Itoh
 */
public class FileDumpStage implements LoadStage<CodecBuffer, Void> {

    private Waiter waiter_;

    private static final int BUFFER_SIZE = 256;

    public FileDumpStage(Waiter waiter) {
        waiter_ = waiter;
    }

    @Override
    public void load(StageContext<Void> context, CodecBuffer input) {
        ByteBuffer in = input.toByteBuffer();
        CharBuffer out = CharBuffer.allocate(BUFFER_SIZE);
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        System.out.println("---");
        for (;;) {
            CoderResult cr = decoder.decode(in, out, in.remaining() == 0);
            if (cr.isOverflow()) {
                System.out.print(out.flip().toString());
                out.clear();
            } else if (cr.isUnderflow()) {
                break;
            }
        }
        out.flip();
        if (out.hasRemaining()) {
            System.out.print(out.toString());
        }
        System.out.println("---");
        waiter_.finished();
    }

    @Override
    public void load(StageContext<Void> context, TransportStateEvent event) {
    }
}
