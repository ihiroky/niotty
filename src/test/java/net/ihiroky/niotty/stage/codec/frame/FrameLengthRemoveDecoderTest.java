package net.ihiroky.niotty.stage.codec.frame;

import net.ihiroky.niotty.LoadStageContextMock;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.DecodeBuffer;
import net.ihiroky.niotty.buffer.EncodeBuffer;
import net.ihiroky.niotty.event.MessageEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.Queue;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class FrameLengthRemoveDecoderTest {

    FrameLengthRemoveDecoder sut;
    LoadStageContextMock<DecodeBuffer, DecodeBuffer> context;
    byte[] data;
    int dataLength;

    @Before
    public void setUp() {
        sut = new FrameLengthRemoveDecoder();
        context = new LoadStageContextMock<>(sut);
        EncodeBuffer encodeBuffer = Buffers.newEncodeBuffer(32);
        encodeBuffer.writeVariableByteInteger(12); // length header + contents
        encodeBuffer.writeInt(1);
        encodeBuffer.writeInt(2);
        encodeBuffer.writeInt(3);
        data = encodeBuffer.toArray();
        dataLength = encodeBuffer.filledBytes();
    }

    @Test
    public void testLoadMessageEventOnce() throws Exception {
        DecodeBuffer input = Buffers.newDecodeBuffer(data, 0, dataLength);

        sut.load(context, new MessageEvent<>(null, input));

        Queue<MessageEvent<DecodeBuffer>> queue = context.getProceededMessageEventQueue();
        DecodeBuffer output = queue.poll().getMessage();
        assertThat(output.remainingBytes(), is(12));
        assertThat(output.readInt(), is(1));
        assertThat(output.readInt(), is(2));
        assertThat(output.readInt(), is(3));
        assertThat(queue.isEmpty(), is(true));
        assertThat(sut.getPooling(), is(nullValue()));
        assertThat(sut.getPoolingFrameBytes(), is(0));
    }

    @Test
    public void testLoadMessageEventManyIncompletePacket() throws Exception {
        // read first 4 byte
        sut.load(context, new MessageEvent<>(null, Buffers.newDecodeBuffer(data, 0, 4)));
        assertThat(context.getProceededMessageEventQueue().size(), is(0));
        assertThat(sut.getPooling().remainingBytes(), is(4));
        assertThat(sut.getPoolingFrameBytes(), is(0));

        // prepend length field is read
        sut.load(context, new MessageEvent<>(null, Buffers.newDecodeBuffer(data, 4, 1)));
        assertThat(context.getProceededMessageEventQueue().size(), is(0));
        assertThat(sut.getPooling().remainingBytes(), is(4));
        assertThat(sut.getPoolingFrameBytes(), is(12));

        // read remaining
        sut.load(context, new MessageEvent<>(null, Buffers.newDecodeBuffer(data, 5, 8)));
        Queue<MessageEvent<DecodeBuffer>> queue = context.getProceededMessageEventQueue();

        DecodeBuffer output = queue.poll().getMessage();
        assertThat(output.remainingBytes(), is(12));
        assertThat(output.readInt(), is(1));
        assertThat(output.readInt(), is(2));
        assertThat(output.readInt(), is(3));
        assertThat(queue.isEmpty(), is(true));
        assertThat(sut.getPooling(), is(nullValue()));
        assertThat(sut.getPoolingFrameBytes(), is(0));
    }

    @Test
    public void testLoadMessageManyPacketAndRemaining() throws Exception {
        EncodeBuffer encodeBuffer = Buffers.newEncodeBuffer(40);
        for (int i = 0; i < 3; i++) {
            encodeBuffer.writeVariableByteInteger(12); // content length
            encodeBuffer.writeInt(1);
            encodeBuffer.writeInt(2);
            encodeBuffer.writeInt(3);
        }
        // remaining 4 bytes
        encodeBuffer.writeVariableByteInteger(12);
        encodeBuffer.writeBytes(new byte[3], 0, 3);
        data = encodeBuffer.toArray();
        dataLength = encodeBuffer.filledBytes();
        DecodeBuffer input = Buffers.newDecodeBuffer(data, 0, dataLength);

        sut.load(context, new MessageEvent<>(null, input));

        Queue<MessageEvent<DecodeBuffer>> queue = context.getProceededMessageEventQueue();
        for (int i = 0; i < 3; i++) {
            MessageEvent<DecodeBuffer> messageEvent = queue.poll();
            DecodeBuffer output = messageEvent.getMessage();
            assertThat(output.readInt(), is(1));
            assertThat(output.readInt(), is(2));
            assertThat(output.readInt(), is(3));
        }
        assertThat(queue.size(), is(0));
        assertThat(input.remainingBytes(), is(0));
        assertThat(sut.getPooling().remainingBytes(), is(4)); // less than MINIMUM_WHOLE_LENGTH
        assertThat(sut.getPoolingFrameBytes(), is(0));
    }

    @Test
    public void testLoadMessageManyPacketAndNoRemaining() throws Exception {
        EncodeBuffer encodeBuffer = Buffers.newEncodeBuffer(40);
        for (int i = 0; i < 3; i++) {
            encodeBuffer.writeVariableByteInteger(12); // content length
            encodeBuffer.writeInt(1);
            encodeBuffer.writeInt(2);
            encodeBuffer.writeInt(3);
        }
        // just 3 packets
        data = encodeBuffer.toArray();
        dataLength = encodeBuffer.filledBytes();
        DecodeBuffer input = Buffers.newDecodeBuffer(data, 0, dataLength);

        sut.load(context, new MessageEvent<>(null, input));

        Queue<MessageEvent<DecodeBuffer>> queue = context.getProceededMessageEventQueue();
        for (int i = 0; i < 3; i++) {
            MessageEvent<DecodeBuffer> messageEvent = queue.poll();
            DecodeBuffer output = messageEvent.getMessage();
            assertThat(output.readInt(), is(1));
            assertThat(output.readInt(), is(2));
            assertThat(output.readInt(), is(3));
        }
        assertThat(queue.size(), is(0));
        assertThat(input.remainingBytes(), is(0));
        assertThat(sut.getPooling(), is(nullValue()));
        assertThat(sut.getPoolingFrameBytes(), is(0));
    }

}
