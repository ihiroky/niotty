package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.buffer.ArrayCodecBuffer;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import org.junit.Before;
import org.junit.Test;

import static net.ihiroky.niotty.codec.ReferenceCountMatcher.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class FrameLengthRemoveDecoderTest {

    FrameLengthRemoveDecoder sut_;
    StageContextMock<CodecBuffer> context_;
    byte[] data_;
    int dataLength_;

    @Before
    public void setUp() {
        sut_ = new FrameLengthRemoveDecoder();
        context_ = new StageContextMock<CodecBuffer>();
        CodecBuffer encodeBuffer = Buffers.newCodecBuffer(32);
        encodeBuffer.writeShort((short) 12);
        encodeBuffer.writeInt(1);
        encodeBuffer.writeInt(2);
        encodeBuffer.writeInt(3);
        data_ = encodeBuffer.array();
        dataLength_ = encodeBuffer.remainingBytes();
    }

    private void setUpAgainAsIntLength() {
        sut_ = new FrameLengthRemoveDecoder();
        context_ = new StageContextMock<CodecBuffer>();
        CodecBuffer encodeBuffer = Buffers.newCodecBuffer(32);
        encodeBuffer.writeInt(12 | 0x80000000); // INT_FLAG
        encodeBuffer.writeInt(1);
        encodeBuffer.writeInt(2);
        encodeBuffer.writeInt(3);
        data_ = encodeBuffer.array();
        dataLength_ = encodeBuffer.remainingBytes();
    }

    @Test
    public void testLoad_MessageShortOnce() throws Exception {
        CodecBuffer input = Buffers.wrap(data_, 0, dataLength_);

        sut_.load(context_, input);

        CodecBuffer output = context_.pollEvent();
        assertThat(output.readInt(), is(1));
        assertThat(output.readInt(), is(2));
        assertThat(output.readInt(), is(3));
        assertThat(context_.hasNoEvent(), is(true));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
        assertThat(input, hasReferenceCount(1));
    }

    @Test
    public void testLoad_MessageIntOnce() throws Exception {
        setUpAgainAsIntLength();

        CodecBuffer input = Buffers.wrap(data_, 0, dataLength_);

        sut_.load(context_, input);

        CodecBuffer output = context_.pollEvent();
        assertThat(output.readInt(), is(1));
        assertThat(output.readInt(), is(2));
        assertThat(output.readInt(), is(3));
        assertThat(context_.hasNoEvent(), is(true));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
        assertThat(input, hasReferenceCount(1));
    }

    @Test
    public void testLoad_MessageManyIncompleteShortPacket() throws Exception {
        // read first 1 byte
        CodecBuffer b = Buffers.wrap(data_, 0, 1);
        sut_.load(context_, b);
        assertThat(context_.eventCount(), is(0));
        assertThat(sut_.getPooling().remainingBytes(), is(1));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
        assertThat(b.remainingBytes(), is(0));
        assertThat(((ArrayCodecBuffer) b).referenceCount(), is(0));

        // prepend length field is read
        b = Buffers.wrap(data_, 1, 1);
        sut_.load(context_, b);
        assertThat(context_.eventCount(), is(0));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(12));
        assertThat(((ArrayCodecBuffer) b).referenceCount(), is(0));

        // read remaining
        b = Buffers.wrap(data_, 2, 12);
        sut_.load(context_, b);

        CodecBuffer output = context_.pollEvent();
        assertThat(output.readInt(), is(1));
        assertThat(output.readInt(), is(2));
        assertThat(output.readInt(), is(3));
        assertThat(context_.hasNoEvent(), is(true));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
        assertThat(b, hasReferenceCount(1));
    }

    @Test
    public void testLoad_MessageManyIncompleteIntPacket() throws Exception {
        setUpAgainAsIntLength();

        // read first byte
        CodecBuffer b = Buffers.wrap(data_, 0, 1);
        sut_.load(context_, b);
        assertThat(context_.eventCount(), is(0));
        assertThat(sut_.getPooling().remainingBytes(), is(1));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
        assertThat(b.remainingBytes(), is(0));
        assertThat(b, hasReferenceCount(0));

        // read second byte
        b = Buffers.wrap(data_, 1, 1);
        sut_.load(context_, b);
        assertThat(context_.eventCount(), is(0));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0x80000000));
        assertThat(b.remainingBytes(), is(0));
        assertThat(b, hasReferenceCount(0));

        // read third byte
        b = Buffers.wrap(data_, 2, 1);
        sut_.load(context_, b);
        assertThat(context_.eventCount(), is(0));
        assertThat(sut_.getPooling().remainingBytes(), is(1));
        assertThat(sut_.getPoolingFrameBytes(), is(0x80000000));
        assertThat(b.remainingBytes(), is(0));
        assertThat(b, hasReferenceCount(0));

        // prepend length field is read
        b = Buffers.wrap(data_, 3, 1);
        sut_.load(context_, b);
        assertThat(context_.eventCount(), is(0));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(12));
        assertThat(b, hasReferenceCount(0));

        // read remaining
        b = Buffers.wrap(data_, 4, 12);
        sut_.load(context_, b);

        CodecBuffer output = context_.pollEvent();
        assertThat(output.readInt(), is(1));
        assertThat(output.readInt(), is(2));
        assertThat(output.readInt(), is(3));
        assertThat(context_.hasNoEvent(), is(true));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
        assertThat(b, hasReferenceCount(1));
    }

    @Test
    public void testLoad_MessageManyPacketAndRemaining() throws Exception {
        CodecBuffer encodeBuffer = Buffers.newCodecBuffer(40);
        final int completePacket = 3;
        for (int i = 0; i < completePacket; i++) {
            encodeBuffer.writeShort((short) 12); // content length
            encodeBuffer.writeInt(1);
            encodeBuffer.writeInt(2);
            encodeBuffer.writeInt(3);
        }
        // remaining 5 bytes
        encodeBuffer.writeShort((short) 12);
        encodeBuffer.writeBytes(new byte[3], 0, 3);
        data_ = encodeBuffer.array();
        dataLength_ = encodeBuffer.remainingBytes();
        CodecBuffer input = Buffers.wrap(data_, 0, dataLength_);

        sut_.load(context_, input);

        for (int i = 0; i < completePacket; i++) {
            CodecBuffer output = context_.pollEvent();
            assertThat(output.readInt(), is(1));
            assertThat(output.readInt(), is(2));
            assertThat(output.readInt(), is(3));
        }
        assertThat(context_.eventCount(), is(0));
        assertThat(input.remainingBytes(), is(0));
        assertThat(sut_.getPooling().remainingBytes(), is(3));
        assertThat(sut_.getPoolingFrameBytes(), is(12));
        assertThat(input, hasReferenceCount(completePacket)); // remaining is drained to the internal buffer
    }

    @Test
    public void testLoad_MessageManyPacketAndNoRemaining() throws Exception {
        CodecBuffer encodeBuffer = Buffers.newCodecBuffer(40);
        final int completePacket = 3;
        for (int i = 0; i < completePacket; i++) {
            encodeBuffer.writeShort((short) 12); // content length
            encodeBuffer.writeInt(1);
            encodeBuffer.writeInt(2);
            encodeBuffer.writeInt(3);
        }
        // just 3 packets
        data_ = encodeBuffer.array();
        dataLength_ = encodeBuffer.remainingBytes();
        CodecBuffer input = Buffers.wrap(data_, 0, dataLength_);

        sut_.load(context_, input);

        for (int i = 0; i < completePacket; i++) {
            CodecBuffer output = context_.pollEvent();
            assertThat(output.readInt(), is(1));
            assertThat(output.readInt(), is(2));
            assertThat(output.readInt(), is(3));
        }
        assertThat(context_.eventCount(), is(0));
        assertThat(input.remainingBytes(), is(0));
        assertThat(sut_.getPooling(), is(nullValue()));
        assertThat(sut_.getPoolingFrameBytes(), is(0));
        assertThat(input, hasReferenceCount(completePacket));
    }

    @Test
    public void testLoad_MessageLargeInput() throws Exception {
        FrameLengthRemoveDecoder sut = new FrameLengthRemoveDecoder();
        CodecBuffer wholeInput = Buffers.newCodecBuffer(8192);
        for (int i = 0; i < 30; i++) {
            CodecBuffer buffer = Buffers.newCodecBuffer(1024);
            for (int j = 0; j < 256; j++) {
                buffer.writeInt(i);
            }
            wholeInput.writeShort((short) (4 * 256));
            wholeInput.drainFrom(buffer);
        }

        while (wholeInput.remainingBytes() > 0) {
            CodecBuffer input = Buffers.newCodecBuffer(8192);
            input.drainFrom(wholeInput, 8192);
            boolean is8192 = input.remainingBytes() == 8192;
            sut.load(context_, input);
            if (is8192) {
                assertThat(input, hasReferenceCount(7));
            } else {
                assertThat(input, hasReferenceCount(6));
            }
        }
        for (int i = 0; i < 30; i++) {
            CodecBuffer b = context_.pollEvent();
            assertThat(b.readInt(), is(i));
        }
        assertThat(context_.hasNoEvent(), is(true));
        assertThat(wholeInput, hasReferenceCount(1));
    }

    @Test
    public void testLoad_RequiredLengthIsNotFulFilledAtOnce() throws Exception {
        byte[] data = new byte[4];

        CodecBuffer input0 = Buffers.newCodecBuffer();
        input0.writeShort((short) 10);
        input0.writeBytes(data, 0, 3);
        sut_.load(context_, input0);
        CodecBuffer input1 = Buffers.newCodecBuffer();
        input1.writeBytes(data, 0, 3);
        sut_.load(context_, input1);
        CodecBuffer input2 = Buffers.newCodecBuffer();
        input2.writeBytes(data, 0, 4);
        sut_.load(context_, input2);

        CodecBuffer proceeded = context_.pollEvent();
        assertThat(proceeded.remainingBytes(), is(10));
        assertThat(proceeded, hasReferenceCount(1));
        assertThat(input0, hasReferenceCount(0));
        assertThat(input1, hasReferenceCount(0));
        assertThat(input2, hasReferenceCount(0));
    }
}
