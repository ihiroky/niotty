package net.ihiroky.niotty.stage.codec.frame;

import net.ihiroky.niotty.StageContextMock;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.DecodeBuffer;
import net.ihiroky.niotty.buffer.EncodeBuffer;
import net.ihiroky.niotty.event.MessageEvent;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class LengthRemoveDecoderTest {

    LengthRemoveDecoder sut;
    StageContextMock<DecodeBuffer, DecodeBuffer> context;
    byte[] data;
    int dataLength;

    @Before
    public void setUp() {
        sut = new LengthRemoveDecoder();
        context = new StageContextMock<>(sut);
        EncodeBuffer encodeBuffer = Buffers.newEncodeBuffer(32);
        encodeBuffer.writeVariableByteInteger(12); // length header + contents
        encodeBuffer.writeInt(1);
        encodeBuffer.writeInt(2);
        encodeBuffer.writeInt(3);
        data = encodeBuffer.toArray();
        dataLength = encodeBuffer.filledBytes();
    }

    @Test
    public void testProcessMessageEventOnce() throws Exception {
        DecodeBuffer input = Buffers.newDecodeBuffer(data, 0, dataLength);

        sut.process(context, new MessageEvent<>(null, input));

        DecodeBuffer output = context.getProceededMessageEvent().getMessage();
        assertThat(output.remainingBytes(), is(12));
        assertThat(output.readInt(), is(1));
        assertThat(output.readInt(), is(2));
        assertThat(output.readInt(), is(3));
    }

    @Test
    public void testProcessMessageEventMany() throws Exception {
        // read first 4 byte
        sut.process(context, new MessageEvent<>(null, Buffers.newDecodeBuffer(data, 0, 4)));
        assertThat(context.getProceededMessageEvent(), is(nullValue()));

        // prepend length field is read
        sut.process(context, new MessageEvent<>(null, Buffers.newDecodeBuffer(data, 4, 1)));
        assertThat(context.getProceededMessageEvent(), is(nullValue()));

        // read remaining
        sut.process(context, new MessageEvent<>(null, Buffers.newDecodeBuffer(data, 5, 8)));
        DecodeBuffer output = context.getProceededMessageEvent().getMessage();

        assertThat(output.remainingBytes(), is(12));
        assertThat(output.readInt(), is(1));
        assertThat(output.readInt(), is(2));
        assertThat(output.readInt(), is(3));
    }
}
