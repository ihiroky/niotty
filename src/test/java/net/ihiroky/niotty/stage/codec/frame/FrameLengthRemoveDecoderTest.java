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

    FrameLengthRemoveDecoder sut_;
    LoadStageContextMock<DecodeBuffer, DecodeBuffer> context_;
    byte[] data_;
    int dataLength_;

    @Before
    public void setUp() {
        sut_ = new FrameLengthRemoveDecoder();
        context_ = new LoadStageContextMock<>(sut_);
        EncodeBuffer encodeBuffer = Buffers.newEncodeBuffer(32);
        encodeBuffer.writeVariableByteInteger(12); // length header + contents
        encodeBuffer.writeInt(1);
        encodeBuffer.writeInt(2);
        encodeBuffer.writeInt(3);
        data_ = encodeBuffer.toArray();
        dataLength_ = encodeBuffer.filledBytes();
    }

    @Test
    public void testLoadMessageEventOnce() throws Exception {
        DecodeBuffer input = Buffers.newDecodeBuffer(data_, 0, dataLength_);

        sut_.load(context_, new MessageEvent<>(null, input));

        Queue<MessageEvent<DecodeBuffer>> queue = context_.getProceededMessageEventQueue();
        DecodeBuffer output = queue.poll().getMessage();
        assertThat(output.remainingBytes(), is(12));
        assertThat(output.readInt(), is(1));
        assertThat(output.readInt(), is(2));
        assertThat(output.readInt(), is(3));
        assertThat(queue.isEmpty(), is(true));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
    }

    @Test
    public void testLoadMessageEventManyIncompletePacket() throws Exception {
        // read first 4 byte
        sut_.load(context_, new MessageEvent<>(null, Buffers.newDecodeBuffer(data_, 0, 4)));
        assertThat(context_.getProceededMessageEventQueue().size(), is(0));
        assertThat(sut_.getPooling().remainingBytes(), is(4));
        assertThat(sut_.getPoolingFrameBytes(), is(0));

        // prepend length field is read
        sut_.load(context_, new MessageEvent<>(null, Buffers.newDecodeBuffer(data_, 4, 1)));
        assertThat(context_.getProceededMessageEventQueue().size(), is(0));
        assertThat(sut_.getPooling().remainingBytes(), is(4));
        assertThat(sut_.getPoolingFrameBytes(), is(12));

        // read remaining
        sut_.load(context_, new MessageEvent<>(null, Buffers.newDecodeBuffer(data_, 5, 8)));
        Queue<MessageEvent<DecodeBuffer>> queue = context_.getProceededMessageEventQueue();

        DecodeBuffer output = queue.poll().getMessage();
        assertThat(output.remainingBytes(), is(12));
        assertThat(output.readInt(), is(1));
        assertThat(output.readInt(), is(2));
        assertThat(output.readInt(), is(3));
        assertThat(queue.isEmpty(), is(true));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
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
        data_ = encodeBuffer.toArray();
        dataLength_ = encodeBuffer.filledBytes();
        DecodeBuffer input = Buffers.newDecodeBuffer(data_, 0, dataLength_);

        sut_.load(context_, new MessageEvent<>(null, input));

        Queue<MessageEvent<DecodeBuffer>> queue = context_.getProceededMessageEventQueue();
        for (int i = 0; i < 3; i++) {
            MessageEvent<DecodeBuffer> messageEvent = queue.poll();
            DecodeBuffer output = messageEvent.getMessage();
            assertThat(output.readInt(), is(1));
            assertThat(output.readInt(), is(2));
            assertThat(output.readInt(), is(3));
        }
        assertThat(queue.size(), is(0));
        assertThat(input.remainingBytes(), is(0));
        assertThat(sut_.getPooling().remainingBytes(), is(4)); // less than MINIMUM_WHOLE_LENGTH
        assertThat(sut_.getPoolingFrameBytes(), is(0));
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
        data_ = encodeBuffer.toArray();
        dataLength_ = encodeBuffer.filledBytes();
        DecodeBuffer input = Buffers.newDecodeBuffer(data_, 0, dataLength_);

        sut_.load(context_, new MessageEvent<>(null, input));

        Queue<MessageEvent<DecodeBuffer>> queue = context_.getProceededMessageEventQueue();
        for (int i = 0; i < 3; i++) {
            MessageEvent<DecodeBuffer> messageEvent = queue.poll();
            DecodeBuffer output = messageEvent.getMessage();
            assertThat(output.readInt(), is(1));
            assertThat(output.readInt(), is(2));
            assertThat(output.readInt(), is(3));
        }
        assertThat(queue.size(), is(0));
        assertThat(input.remainingBytes(), is(0));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
    }

}
