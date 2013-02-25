package net.ihiroky.niotty.stage.codec.frame;

import net.ihiroky.niotty.StoreStageContextMock;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.EncodeBuffer;
import net.ihiroky.niotty.buffer.EncodeBufferGroup;
import net.ihiroky.niotty.event.MessageEvent;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class LengthPrependEncoderTest {

    LengthPrependEncoder sut;
    StoreStageContextMock<EncodeBufferGroup, EncodeBufferGroup> context;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        sut = new LengthPrependEncoder();
        context = new StoreStageContextMock<>(sut);
    }

    @Test
    public void testProcessMessageEvent() throws Exception {
        EncodeBuffer input = Buffers.newEncodeBuffer(10);
        input.writeBytes(new byte[5], 0, 5);
        EncodeBufferGroup group = new EncodeBufferGroup();
        group.addLast(input);

        sut.store(context, new MessageEvent<>(null, group));

        EncodeBufferGroup actual = context.getProceededMessageEvent().getMessage();
        assertThat(actual.filledBytes(), is(6));
    }

    @Test
    public void testProcessMessageEventLessThan5() throws Exception {
        EncodeBuffer input = Buffers.newEncodeBuffer(10);
        input.writeBytes(new byte[3], 0, 3);
        EncodeBufferGroup group = new EncodeBufferGroup();
        group.addLast(input);

        sut.store(context, new MessageEvent<>(null, group));

        EncodeBufferGroup actual = context.getProceededMessageEvent().getMessage();
        assertThat(actual.filledBytes(), is(5));
    }
}
