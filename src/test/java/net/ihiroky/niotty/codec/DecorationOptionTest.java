package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.util.Charsets;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DecorationOptionTest {

    @Test
    public void testPrependTimestamp() throws Exception {
        DecorationOption sut = new DecorationOption(true, false, null);
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer actual = (ByteBuffer) invocation.getArguments()[0];
                String actualString = Charsets.US_ASCII.newDecoder().decode(actual.duplicate()).toString();
                String expected = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS ").format(new Date(0L));
                assertThat(actualString, is(expected));
                return 0;
            }
        });

        sut.prepend(channel, 0, 0L);

        verify(channel).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testPrependPacketLength() throws Exception {
        DecorationOption sut = new DecorationOption(false, true, null);
        final int packetLength = 4;
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer actual = (ByteBuffer) invocation.getArguments()[0];
                ByteBuffer expected = ByteBuffer.allocate(4);
                expected.putInt(packetLength).flip();
                assertThat(actual, is(expected));
                return 0;
            }
        });

        sut.prepend(channel, packetLength, 0L);

        verify(channel).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testAppendSeparator() throws Exception {
        final byte[] separator = new byte[]{'\r', '\n'};
        DecorationOption sut = new DecorationOption(false, false, separator);
        GatheringByteChannel channel = mock(GatheringByteChannel.class);
        when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer actual = (ByteBuffer) invocation.getArguments()[0];
                ByteBuffer expected = ByteBuffer.wrap(separator);
                assertThat(actual, is(expected));
                return 0;
            }
        });

        sut.append(channel, 0, 0L);

        verify(channel).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testAppend0999() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(4);

        DecorationOption.append0000(buffer, 999);

        buffer.flip();
        assertThat(buffer, is(ByteBuffer.wrap(new byte[]{'0', '9', '9', '9'})));
    }

    @Test
    public void testAppend0099() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(4);

        DecorationOption.append0000(buffer, 99);

        buffer.flip();
        assertThat(buffer, is(ByteBuffer.wrap(new byte[]{'0', '0', '9', '9'})));
    }

    @Test
    public void testAppend0009() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(4);

        DecorationOption.append0000(buffer, 99);

        buffer.flip();
        assertThat(buffer, is(ByteBuffer.wrap(new byte[]{'0', '0', '9', '9'})));
    }
}
