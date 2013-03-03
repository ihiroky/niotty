package net.ihiroky.niotty.stage.codec.frame;

import net.ihiroky.niotty.LoadStageContextMock;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
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
    LoadStageContextMock<CodecBuffer, CodecBuffer> context_;
    byte[] data_;
    int dataLength_;

    @Before
    public void setUp() {
        setUp(false);
    }

    private void setUp(boolean useSlice) {
        sut_ = new FrameLengthRemoveDecoder(useSlice);
        context_ = new LoadStageContextMock<>(sut_);
        CodecBuffer encodeBuffer = Buffers.newCodecBuffer(32);
        encodeBuffer.writeVariableByteInteger(12); // length header + contents
        encodeBuffer.writeInt(1);
        encodeBuffer.writeInt(2);
        encodeBuffer.writeInt(3);
        data_ = encodeBuffer.toArray();
        dataLength_ = encodeBuffer.remainingBytes();
    }

    @Test
    public void testLoadMessageEventOnce() throws Exception {
        CodecBuffer input = Buffers.newCodecBuffer(data_, 0, dataLength_);

        sut_.load(context_, new MessageEvent<>(null, input));

        Queue<MessageEvent<CodecBuffer>> queue = context_.getProceededMessageEventQueue();
        CodecBuffer output = queue.poll().getMessage();
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
        sut_.load(context_, new MessageEvent<>(null, Buffers.newCodecBuffer(data_, 0, 4)));
        assertThat(context_.getProceededMessageEventQueue().size(), is(0));
        assertThat(sut_.getPooling().remainingBytes(), is(4));
        assertThat(sut_.getPoolingFrameBytes(), is(0));

        // prepend length field is read
        sut_.load(context_, new MessageEvent<>(null, Buffers.newCodecBuffer(data_, 4, 1)));
        assertThat(context_.getProceededMessageEventQueue().size(), is(0));
        assertThat(sut_.getPooling().remainingBytes(), is(4));
        assertThat(sut_.getPoolingFrameBytes(), is(12));

        // read remaining
        sut_.load(context_, new MessageEvent<>(null, Buffers.newCodecBuffer(data_, 5, 8)));
        Queue<MessageEvent<CodecBuffer>> queue = context_.getProceededMessageEventQueue();

        CodecBuffer output = queue.poll().getMessage();
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
        CodecBuffer encodeBuffer = Buffers.newCodecBuffer(40);
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
        dataLength_ = encodeBuffer.remainingBytes();
        CodecBuffer input = Buffers.newCodecBuffer(data_, 0, dataLength_);

        sut_.load(context_, new MessageEvent<>(null, input));

        Queue<MessageEvent<CodecBuffer>> queue = context_.getProceededMessageEventQueue();
        for (int i = 0; i < 3; i++) {
            MessageEvent<CodecBuffer> messageEvent = queue.poll();
            CodecBuffer output = messageEvent.getMessage();
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
        CodecBuffer encodeBuffer = Buffers.newCodecBuffer(40);
        for (int i = 0; i < 3; i++) {
            encodeBuffer.writeVariableByteInteger(12); // content length
            encodeBuffer.writeInt(1);
            encodeBuffer.writeInt(2);
            encodeBuffer.writeInt(3);
        }
        // just 3 packets
        data_ = encodeBuffer.toArray();
        dataLength_ = encodeBuffer.remainingBytes();
        CodecBuffer input = Buffers.newCodecBuffer(data_, 0, dataLength_);

        sut_.load(context_, new MessageEvent<>(null, input));

        Queue<MessageEvent<CodecBuffer>> queue = context_.getProceededMessageEventQueue();
        for (int i = 0; i < 3; i++) {
            MessageEvent<CodecBuffer> messageEvent = queue.poll();
            CodecBuffer output = messageEvent.getMessage();
            assertThat(output.readInt(), is(1));
            assertThat(output.readInt(), is(2));
            assertThat(output.readInt(), is(3));
        }
        assertThat(queue.size(), is(0));
        assertThat(input.remainingBytes(), is(0));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
    }

    @Test
    public void testLoadMessageManyPacketAndNoRemainingWithUseSlice() throws Exception {
        setUp(true);
        testLoadMessageManyPacketAndNoRemaining();
    }

}
