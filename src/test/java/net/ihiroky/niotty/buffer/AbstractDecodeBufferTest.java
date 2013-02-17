package net.ihiroky.niotty.buffer;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class AbstractDecodeBufferTest {

    @Test
    public void testReadVariableByteNumber0() throws Exception {
        AbstractDecodeBuffer sut = ArrayDecodeBuffer.wrap(new byte[]{(byte) 0x80}, 1);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual.intValue(), is(0));
    }

    @Test
    public void testReadVariableByteNumber1() throws Exception {
        AbstractDecodeBuffer sut = ArrayDecodeBuffer.wrap(new byte[]{(byte) 0x81}, 1);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual.intValue(), is(1));
    }

    @Test
    public void testReadVariableByteNumberMinus1() throws Exception {
        AbstractDecodeBuffer sut = ArrayDecodeBuffer.wrap(new byte[]{(byte) 0xc1}, 1);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual.intValue(), is(-1));
    }

    @Test
    public void testReadVariableByteNumber229() throws Exception {
        byte[] data = new byte[]{0x25, (byte) 0x83};
        AbstractDecodeBuffer sut = ArrayDecodeBuffer.wrap(data, data.length);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual.intValue(), is(0b11100101));
    }

    @Test
    public void testReadVariableByteNumberMinus229() throws Exception {
        byte[] data = new byte[]{0x65, (byte) 0x83};
        AbstractDecodeBuffer sut = ArrayDecodeBuffer.wrap(data, data.length);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual.intValue(), is(-0b11100101));
    }

    @Test
    public void testReadVariableByteNumberNull() throws Exception {
        AbstractDecodeBuffer sut = ArrayDecodeBuffer.wrap(new byte[]{(byte) 0xc0}, 1);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testReadVariableByteNumberMinValue() throws Exception {
        byte[] data = new byte[] {
                0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x82
        };
        AbstractDecodeBuffer sut = ArrayDecodeBuffer.wrap(data, data.length);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual.longValue(), is(Long.MIN_VALUE));
    }

    @Test
    public void testReadVariableByte0() throws Exception {
        AbstractDecodeBuffer sut = ArrayDecodeBuffer.wrap(new byte[]{(byte) 0x80}, 1);
        long actual = sut.readVariableByte();
        assertThat(actual, is(0L));
    }

    @Test
    public void testReadVariableByte1() throws Exception {
        AbstractDecodeBuffer sut = ArrayDecodeBuffer.wrap(new byte[]{(byte) 0x81}, 1);
        long actual = sut.readVariableByte();
        assertThat(actual, is(1L));
    }

    @Test
    public void testReadVariableByteMinus0() throws Exception {
        AbstractDecodeBuffer sut = ArrayDecodeBuffer.wrap(new byte[]{(byte) 0xc0}, 1);
        long actual = sut.readVariableByte();
        assertThat(actual, is(0L));
    }

    @Test
    public void testReadVariableByteMinus1() throws Exception {
        AbstractDecodeBuffer sut = ArrayDecodeBuffer.wrap(new byte[]{(byte) 0xc1}, 1);
        long actual = sut.readVariableByte();
        assertThat(actual, is(-1L));
    }
}
