package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class AbstractEncodeBufferTest {

    private ArrayEncodeBuffer sut;

    @Before
    public void setUp() {
        sut = new ArrayEncodeBuffer(0);
    }

    @Test
    public void testWriteVariableByteNull() throws Exception {
        sut.writeVariableByteNull();
        assertThat(sut.array()[0], is((byte) 0xc0));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByte0() throws Exception {
        sut.writeVariableByte(0);
        assertThat(sut.array()[0], is((byte) 0x80));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByte1() throws Exception {
        sut.writeVariableByte(1);
        assertThat(sut.array()[0], is((byte) 0x81));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByteMinValue() throws Exception {
        sut.writeVariableByte(Long.MIN_VALUE);
        assertThat(Arrays.copyOf(sut.array(), 10), is(new byte[] {
                0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x82,
        }));
        assertThat(sut.filledBytes(), is(10));
    }

    @Test
    public void testWriteVariableByte229() throws Exception {
        sut.writeVariableByte(229);
        assertThat(Arrays.copyOf(sut.array(), 2), is(new byte[]{0x25, (byte) 0x83}));
        assertThat(sut.filledBytes(), is(2));
    }

    @Test
    public void testWriteVariableByteMinux229() throws Exception {
        sut.writeVariableByte(-229);
        assertThat(Arrays.copyOf(sut.array(), 2), is(new byte[]{0x65, (byte) 0x83}));
        assertThat(sut.filledBytes(), is(2));
    }

    @Test
    public void testWriteVariableByteInteger() throws Exception {
        sut.writeVariableByteInteger(1);
        assertThat(sut.array()[0], is((byte) 0x81));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByteIntegerNull() throws Exception {
        sut.writeVariableByteInteger(null);
        assertThat(sut.array()[0], is((byte) 0xc0));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByteLong() throws Exception {
        sut.writeVariableByteLong(1L);
        assertThat(sut.array()[0], is((byte) 0x81));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByteLongNull() throws Exception {
        sut.writeVariableByteLong(null);
        assertThat(sut.array()[0], is((byte) 0xc0));
        assertThat(sut.filledBytes(), is(1));
    }
}
