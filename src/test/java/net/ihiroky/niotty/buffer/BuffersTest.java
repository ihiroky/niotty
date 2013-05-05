package net.ihiroky.niotty.buffer;

import org.junit.Test;

import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class BuffersTest {
    @Test
    public void testOutputCharBufferSize() throws Exception {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        assertThat(Buffers.outputCharBufferSize(decoder, 10), is(11));
        assertThat(Buffers.outputCharBufferSize(decoder, 0), is(2));
    }

    @Test
    public void testExpand() throws Exception {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        CharBuffer b = CharBuffer.wrap("0123");
        b.position(4);
        CharBuffer expanded = Buffers.expand(b, decoder, 10);
        assertThat(expanded.capacity(), is(15));
        assertThat(expanded.limit(), is(15));
        assertThat(expanded.position(), is(4));

        b = CharBuffer.wrap("0123");
        b.position(3);
        expanded = Buffers.expand(b, decoder, 2);
        assertThat(expanded.capacity(), is(7));
        assertThat(expanded.limit(), is(7));
        assertThat(expanded.position(), is(3));
    }

    @Test
    public void testThrowRuntimeException() throws Exception {
        try {
            Buffers.throwRuntimeException(CoderResult.unmappableForLength(1));
            fail();
        } catch (RuntimeException re) {
            assertThat(re.getCause(), is(instanceOf(UnmappableCharacterException.class)));
        }

        try {
            Buffers.throwRuntimeException(CoderResult.malformedForLength(1));
            fail();
        } catch (RuntimeException re) {
            assertThat(re.getCause(), is(instanceOf(MalformedInputException.class)));
        }
    }
}
