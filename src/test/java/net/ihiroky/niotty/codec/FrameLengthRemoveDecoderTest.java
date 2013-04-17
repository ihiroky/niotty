package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.LoadStageContextMock;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
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
        sut_ = new FrameLengthRemoveDecoder();
        context_ = new LoadStageContextMock<>(sut_);
        CodecBuffer encodeBuffer = Buffers.newCodecBuffer(32);
        encodeBuffer.writeShort((short) 12);
        encodeBuffer.writeInt(1);
        encodeBuffer.writeInt(2);
        encodeBuffer.writeInt(3);
        data_ = encodeBuffer.toArray();
        dataLength_ = encodeBuffer.remainingBytes();
    }

    @Test
    public void testLoad_MessageOnce() throws Exception {
        CodecBuffer input = Buffers.newCodecBuffer(data_, 0, dataLength_);

        sut_.load(context_, input);

        Queue<CodecBuffer> queue = context_.getProceededMessageEventQueue();
        CodecBuffer output = queue.poll();
        assertThat(output.readInt(), is(1));
        assertThat(output.readInt(), is(2));
        assertThat(output.readInt(), is(3));
        assertThat(queue.isEmpty(), is(true));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
    }

    @Test
    public void testLoad_MessageManyIncompletePacket() throws Exception {
        // read first 1 byte
        CodecBuffer b = Buffers.newCodecBuffer(data_, 0, 1);
        sut_.load(context_, b);
        assertThat(context_.getProceededMessageEventQueue().size(), is(0));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
        assertThat(b.remainingBytes(), is(1));

        // prepend length field is read
        sut_.load(context_, Buffers.newCodecBuffer(data_, 0, 2));
        assertThat(context_.getProceededMessageEventQueue().size(), is(0));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(12));

        // read remaining
        sut_.load(context_, Buffers.newCodecBuffer(data_, 2, 12));
        Queue<CodecBuffer> queue = context_.getProceededMessageEventQueue();

        CodecBuffer output = queue.poll();
        assertThat(output.readInt(), is(1));
        assertThat(output.readInt(), is(2));
        assertThat(output.readInt(), is(3));
        assertThat(queue.isEmpty(), is(true));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
    }

    @Test
    public void testLoad_MessageManyPacketAndRemaining() throws Exception {
        CodecBuffer encodeBuffer = Buffers.newCodecBuffer(40);
        for (int i = 0; i < 3; i++) {
            encodeBuffer.writeShort((short) 12); // content length
            encodeBuffer.writeInt(1);
            encodeBuffer.writeInt(2);
            encodeBuffer.writeInt(3);
        }
        // remaining 5 bytes
        encodeBuffer.writeShort((short) 12);
        encodeBuffer.writeBytes(new byte[3], 0, 3);
        data_ = encodeBuffer.toArray();
        dataLength_ = encodeBuffer.remainingBytes();
        CodecBuffer input = Buffers.newCodecBuffer(data_, 0, dataLength_);

        sut_.load(context_, input);

        Queue<CodecBuffer> queue = context_.getProceededMessageEventQueue();
        for (int i = 0; i < 3; i++) {
            CodecBuffer output = queue.poll();
            assertThat(output.readInt(), is(1));
            assertThat(output.readInt(), is(2));
            assertThat(output.readInt(), is(3));
        }
        assertThat(queue.size(), is(0));
        assertThat(input.remainingBytes(), is(0));
        assertThat(sut_.getPooling().remainingBytes(), is(3));
        assertThat(sut_.getPoolingFrameBytes(), is(12));
    }

    @Test
    public void testLoad_MessageManyPacketAndNoRemaining() throws Exception {
        CodecBuffer encodeBuffer = Buffers.newCodecBuffer(40);
        for (int i = 0; i < 3; i++) {
            encodeBuffer.writeShort((short) 12); // content length
            encodeBuffer.writeInt(1);
            encodeBuffer.writeInt(2);
            encodeBuffer.writeInt(3);
        }
        // just 3 packets
        data_ = encodeBuffer.toArray();
        dataLength_ = encodeBuffer.remainingBytes();
        CodecBuffer input = Buffers.newCodecBuffer(data_, 0, dataLength_);

        sut_.load(context_, input);

        Queue<CodecBuffer> queue = context_.getProceededMessageEventQueue();
        for (int i = 0; i < 3; i++) {
            CodecBuffer output = queue.poll();
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
    public void testLoad_MessageLargeInput() throws Exception {
        FrameLengthRemoveDecoder sut = new FrameLengthRemoveDecoder();
        CodecBuffer wholeInput = Buffers.newCodecBuffer(8192);
        for (int i = 0; i < 30; i++) {
            CodecBuffer buffer = Buffers.newCodecBuffer(1024, -((i + 1) % 2));
            for (int j = 0; j < 256; j++) {
                buffer.writeInt(i);
            }
            wholeInput.writeShort((short) (4 * 256));
            wholeInput.drainFrom(buffer);
        }

        while (wholeInput.remainingBytes() > 0) {
            CodecBuffer input = Buffers.newCodecBuffer(8192);
            input.drainFrom(wholeInput, 8192);
            sut.load(context_, input);
        }
        for (int i = 0; i < 30; i++) {
            CodecBuffer b = context_.getProceededMessageEventQueue().poll();
            assertThat(b.readInt(), is(i));
        }
        assertThat(context_.getProceededMessageEventQueue().isEmpty(), is(true));
    }
}
