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
        assertThat(sut.toArray()[0], is((byte) 0xc0));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByteInteger0() throws Exception {
        sut.writeVariableByteInteger(0);
        assertThat(sut.toArray()[0], is((byte) 0x80));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByteLong0() throws Exception {
        sut.writeVariableByteLong(0);
        assertThat(sut.toArray()[0], is((byte) 0x80));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByte1() throws Exception {
        sut.writeVariableByteInteger(1);
        assertThat(sut.toArray()[0], is((byte) 0x81));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByteLong1() throws Exception {
        sut.writeVariableByteLong(1);
        assertThat(sut.toArray()[0], is((byte) 0x81));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByteIntegerMinValue() throws Exception {
        sut.writeVariableByteInteger(Integer.MIN_VALUE);
        assertThat(Arrays.copyOf(sut.toArray(), 5), is(new byte[] {
                0x40, 0x00, 0x00, 0x00, (byte) 0x90,
        }));
        assertThat(sut.filledBytes(), is(5));
    }

    @Test
    public void testWriteVariableByteLongMinValue() throws Exception {
        sut.writeVariableByteLong(Long.MIN_VALUE);
        assertThat(Arrays.copyOf(sut.toArray(), 10), is(new byte[] {
                0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x82,
        }));
        assertThat(sut.filledBytes(), is(10));
    }

    @Test
    public void testWriteVariableByteInteger229() throws Exception {
        sut.writeVariableByteInteger(229);
        assertThat(Arrays.copyOf(sut.toArray(), 2), is(new byte[]{0x25, (byte) 0x83}));
        assertThat(sut.filledBytes(), is(2));
    }

    @Test
    public void testWriteVariableByteLong229() throws Exception {
        sut.writeVariableByteLong(229);
        assertThat(Arrays.copyOf(sut.toArray(), 2), is(new byte[]{0x25, (byte) 0x83}));
        assertThat(sut.filledBytes(), is(2));
    }

    @Test
    public void testWriteVariableByteIntegerMinus229() throws Exception {
        sut.writeVariableByteInteger(-229);
        assertThat(Arrays.copyOf(sut.toArray(), 2), is(new byte[]{0x65, (byte) 0x83}));
        assertThat(sut.filledBytes(), is(2));
    }

    @Test
    public void testWriteVariableByteLongMinus229() throws Exception {
        sut.writeVariableByteLong(-229);
        assertThat(Arrays.copyOf(sut.toArray(), 2), is(new byte[]{0x65, (byte) 0x83}));
        assertThat(sut.filledBytes(), is(2));
    }

    @Test
    public void testWriteVariableByteIntegerObj() throws Exception {
        sut.writeVariableByteInteger(Integer.valueOf(1));
        assertThat(sut.toArray()[0], is((byte) 0x81));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByteIntegerNull() throws Exception {
        sut.writeVariableByteInteger(null);
        assertThat(sut.toArray()[0], is((byte) 0xc0));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByteLongObj() throws Exception {
        sut.writeVariableByteLong(Long.valueOf(1L));
        assertThat(sut.toArray()[0], is((byte) 0x81));
        assertThat(sut.filledBytes(), is(1));
    }

    @Test
    public void testWriteVariableByteLongNull() throws Exception {
        sut.writeVariableByteLong(null);
        assertThat(sut.toArray()[0], is((byte) 0xc0));
        assertThat(sut.filledBytes(), is(1));
    }
}
