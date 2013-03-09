package net.ihiroky.niotty.stage.codec.frame;

import net.ihiroky.niotty.StoreStageContextMock;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.buffer.CodecBufferDeque;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class FrameLengthPrependEncoderTest {

    FrameLengthPrependEncoder sut_;
    StoreStageContextMock<CodecBufferDeque, CodecBufferDeque> context_;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        sut_ = new FrameLengthPrependEncoder();
        context_ = new StoreStageContextMock<>(sut_);
    }

    @Test
    public void testProcessMessageEvent() throws Exception {
        CodecBuffer input = Buffers.newCodecBuffer(10);
        input.writeBytes(new byte[5], 0, 5);
        CodecBufferDeque group = new CodecBufferDeque();
        group.addLast(input);

        sut_.store(context_, group);

        CodecBufferDeque actual = context_.getProceededMessageEvent().poll();
        assertThat(actual.remainingBytes(), is(6));
    }

    @Test
    public void testProcessMessageEventLessThan5() throws Exception {
        CodecBuffer input = Buffers.newCodecBuffer(10);
        input.writeBytes(new byte[3], 0, 3);
        CodecBufferDeque group = new CodecBufferDeque();
        group.addLast(input);

        sut_.store(context_, group);

        CodecBufferDeque actual = context_.getProceededMessageEvent().poll();
        assertThat(actual.remainingBytes(), is(5));
    }
}
