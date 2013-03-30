package net.ihiroky.niotty.codec.frame;

import net.ihiroky.niotty.StoreStageContextMock;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class FrameLengthPrependEncoderTest {

    FrameLengthPrependEncoder sut_;
    StoreStageContextMock<BufferSink, BufferSink> context_;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        sut_ = new FrameLengthPrependEncoder();
        context_ = new StoreStageContextMock<>(sut_);
    }

    @Test
    public void testProcess_Message() throws Exception {
        CodecBuffer input = Buffers.newCodecBuffer(10);
        input.writeBytes(new byte[5], 0, 5);

        sut_.store(context_, input);

        BufferSink actual = context_.getProceededMessageEvent().poll();
        assertThat(actual.remainingBytes(), is(6));
    }
}
