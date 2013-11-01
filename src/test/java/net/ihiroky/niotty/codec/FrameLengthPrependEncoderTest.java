package net.ihiroky.niotty.codec;

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
    StageContextMock<BufferSink> context_;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        sut_ = new FrameLengthPrependEncoder();
        context_ = new StageContextMock<BufferSink>();
    }

    @Test
    public void testStore_Message() throws Exception {
        CodecBuffer input = Buffers.newCodecBuffer(10);
        input.writeBytes(new byte[5], 0, 5);

        sut_.store(context_, input);

        BufferSink actual = context_.pollEvent();
        assertThat(actual.remaining(), is(7));
    }

    @Test
    public void testStore_MessageLength32767() throws Exception {
        CodecBuffer input = Buffers.newCodecBuffer(Short.MAX_VALUE);
        input.writeBytes(new byte[Short.MAX_VALUE], 0, Short.MAX_VALUE);

        sut_.store(context_, input);

        BufferSink actual = context_.pollEvent();
        assertThat(actual.remaining(), is(Short.MAX_VALUE + 2));
    }

    @Test
    public void testStore_MessageLength32768() throws Exception {
        CodecBuffer input = Buffers.newCodecBuffer(Short.MAX_VALUE + 1);
        input.writeBytes(new byte[Short.MAX_VALUE + 1], 0, Short.MAX_VALUE + 1);

        sut_.store(context_, input);

        BufferSink actual = context_.pollEvent();
        assertThat(actual.remaining(), is((Short.MAX_VALUE + 1) + 4));
    }
}