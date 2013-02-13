package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class ArrayEncodeBufferTest {

    private ArrayEncodeBuffer sut;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        sut = new ArrayEncodeBuffer(8);
    }

    @Test
    public void testConstructorSizeUnder8() throws Exception {
        ArrayEncodeBuffer localSut = new ArrayEncodeBuffer(7);
        assertThat(localSut.capacityBytes(), is(8));

        localSut = new ArrayEncodeBuffer(8);
        assertThat(localSut.capacityBytes(), is(8));
    }

    @Test
    public void testConstructorDefault() throws Exception {
        ArrayEncodeBuffer localSut = new ArrayEncodeBuffer();
        assertThat(localSut.capacityBytes(), is(512));
    }

    @Test
    public void testWriteByte() throws Exception {
        sut.writeByte(10);
        assertThat(sut.array()[0], is((byte) 10));

        sut.writeByte(20);
        assertThat(sut.array()[1], is((byte) 20));
    }

    @Test
    public void testWriteBytesExpand() throws Exception {
        for (int i = 0; i < 9; i++) {
            sut.writeByte(0);
        }
        assertThat(sut.filledBytes(), is(9));
        assertThat(sut.capacityBytes(), is(16));
    }

    @Test
    public void testWriteBytesWhole() throws Exception {
        byte[] b = new byte[] {'0', '1', '2'};
        sut.writeBytes(b, 0, 3);
        assertThat(sut.filledBytes(), is(3));
        assertThat(sut.array()[0], is((byte) 0x30));
        assertThat(sut.array()[1], is((byte) 0x31));
        assertThat(sut.array()[2], is((byte) 0x32));
    }

    @Test
    public void testWriteBytesWholeExpand() throws Exception {
        byte[] b = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        sut.writeBytes(b, 0, 10);
        assertThat(sut.filledBytes(), is(10));
        assertThat(sut.array()[0], is((byte) 0x30));
        assertThat(sut.array()[1], is((byte) 0x31));
        assertThat(sut.array()[8], is((byte) 0x38));
        assertThat(sut.array()[9], is((byte) 0x39));
    }

    @Test
    public void testWriteBytesHead() throws Exception {
        byte[] b = new byte[] {'0', '1', '2'};
        sut.writeBytes(b, 0, 2);
        assertThat(sut.filledBytes(), is(2));
        assertThat(sut.array()[0], is((byte) 0x30));
        assertThat(sut.array()[1], is((byte) 0x31));
    }

    @Test
    public void testWriteBytesHeadExpand() throws Exception {
        byte[] b = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        sut.writeBytes(b, 0, 9);
        assertThat(sut.filledBytes(), is(9));
        assertThat(sut.array()[0], is((byte) 0x30));
        assertThat(sut.array()[1], is((byte) 0x31));
        assertThat(sut.array()[7], is((byte) 0x37));
        assertThat(sut.array()[8], is((byte) 0x38));
    }

    @Test
    public void testWriteBytesTail() throws Exception {
        byte[] b = new byte[] {'0', '1', '2'};
        sut.writeBytes(b, 1, 2);
        assertThat(sut.filledBytes(), is(2));
        assertThat(sut.array()[0], is((byte) 0x31));
        assertThat(sut.array()[1], is((byte) 0x32));
    }

    @Test
    public void testWriteBytesTailExpand() throws Exception {
        byte[] b = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        sut.writeBytes(b, 1, 9);
        assertThat(sut.filledBytes(), is(9));
        assertThat(sut.array()[0], is((byte) 0x31));
        assertThat(sut.array()[1], is((byte) 0x32));
        assertThat(sut.array()[7], is((byte) 0x38));
        assertThat(sut.array()[8], is((byte) 0x39));
    }

    @Test
    public void testWriteBytes4() throws Exception {
        sut.writeBytes4(-1, 0);
        assertThat(sut.filledBytes(), is(0));

        sut.writeBytes4(-1, 2);
        assertThat(sut.filledBytes(), is(2));
        assertThat(sut.array()[0], is((byte) 0xFF));
        assertThat(sut.array()[1], is((byte) 0xFF));

        sut.clear();
        sut.writeBytes4(-1, 4);
        assertThat(sut.filledBytes(), is(4));
        assertThat(sut.array()[0], is((byte) 0xFF));
        assertThat(sut.array()[1], is((byte) 0xFF));
        assertThat(sut.array()[2], is((byte) 0xFF));
        assertThat(sut.array()[3], is((byte) 0xFF));
    }

    @Test
    public void testWriteBytes4UnderRange() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        sut.writeBytes4(-1, -1);
    }

    @Test
    public void testWriteBytes4OverRange() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        sut.writeBytes4(-1, 5);
    }

    @Test
    public void testWriteBytes8() throws Exception {
        sut.writeBytes8(-1, 0);
        assertThat(sut.filledBytes(), is(0));

        sut.writeBytes8(-1, 2);
        assertThat(sut.filledBytes(), is(2));
        assertThat(sut.array()[0], is((byte) 0xFF));
        assertThat(sut.array()[1], is((byte) 0xFF));

        sut.clear();
        sut.writeBytes8(-1, 8);
        assertThat(sut.filledBytes(), is(8));
        assertThat(sut.array()[0], is((byte) 0xFF));
        assertThat(sut.array()[1], is((byte) 0xFF));
        assertThat(sut.array()[2], is((byte) 0xFF));
        assertThat(sut.array()[3], is((byte) 0xFF));
        assertThat(sut.array()[4], is((byte) 0xFF));
        assertThat(sut.array()[5], is((byte) 0xFF));
        assertThat(sut.array()[6], is((byte) 0xFF));
        assertThat(sut.array()[7], is((byte) 0xFF));
    }

    @Test
    public void testWriteBytes8UnderRange() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        sut.writeBytes8(-1, -1);
    }

    @Test
    public void testWriteBytes8OverRange() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        sut.writeBytes4(-1, 9);
    }

    @Test
    public void testWriteShort() throws Exception {
        sut.writeShort(Short.MAX_VALUE);
        byte[] b = sut.array();
        assertThat(b[0], is((byte) 0x7F));
        assertThat(b[1], is((byte) 0xFF));
        assertThat(sut.filledBytes(), is(2));
    }

    @Test
    public void testWriteChar() throws Exception {
        sut.writeChar(Character.MAX_VALUE);
        byte[] b = sut.array();
        assertThat(b[0], is((byte) 0xFF));
        assertThat(b[1], is((byte) 0xFF));
        assertThat(sut.filledBytes(), is(2));
    }

    @Test
    public void testWriteInt() throws Exception {
        sut.writeInt(Integer.MIN_VALUE);
        byte[] b = sut.array();
        assertThat(b[0], is((byte) 0x80));
        assertThat(b[1], is((byte) 0x00));
        assertThat(b[2], is((byte) 0x00));
        assertThat(b[3], is((byte) 0x00));
        assertThat(sut.filledBytes(), is(4));
    }

    @Test
    public void testWriteLong() throws Exception {
        sut.writeLong(1);
        byte[] b = sut.array();
        assertThat(b[0], is((byte) 0x00));
        assertThat(b[1], is((byte) 0x00));
        assertThat(b[2], is((byte) 0x00));
        assertThat(b[3], is((byte) 0x00));
        assertThat(b[4], is((byte) 0x00));
        assertThat(b[5], is((byte) 0x00));
        assertThat(b[6], is((byte) 0x00));
        assertThat(b[7], is((byte) 0x01));
        assertThat(sut.filledBytes(), is(8));
    }

    @Test
    public void testWriteFloat() throws Exception {
        sut.writeFloat(1f);
        byte[] b = sut.array();
        assertThat(b[0], is((byte) 0x3F));
        assertThat(b[1], is((byte) 0x80));
        assertThat(b[2], is((byte) 0x00));
        assertThat(b[3], is((byte) 0x00));
        assertThat(sut.filledBytes(), is(4));
    }

    @Test
    public void testWriteDouble() throws Exception {
        sut.writeDouble(1d);
        byte[] b = sut.array();
        assertThat(b[0], is((byte) 0x3F));
        assertThat(b[1], is((byte) 0xF0));
        assertThat(b[2], is((byte) 0x00));
        assertThat(b[3], is((byte) 0x00));
        assertThat(b[4], is((byte) 0x00));
        assertThat(b[5], is((byte) 0x00));
        assertThat(b[6], is((byte) 0x00));
        assertThat(b[7], is((byte) 0x00));
        assertThat(sut.filledBytes(), is(8));
    }

    @Test
    public void testDrainTo() throws Exception {
        sut.writeInt(0x01020304);
        sut.writeInt(0x05060708);
        ArrayEncodeBuffer b = new ArrayEncodeBuffer(8);

        sut.drainTo(b);

        assertThat(sut.filledBytes(), is(0));
        assertThat(b.filledBytes(), is(8));
        assertThat(Arrays.copyOf(b.array(), 8), is(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}));
        assertThat(b.capacityBytes(), is(16));
    }
}
