package net.ihiroky.niotty.buffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class ArrayEncodeBufferTest {

    private ArrayEncodeBuffer sut_;

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Before
    public void setUp() {
        sut_ = new ArrayEncodeBuffer(8);
    }

    @Test
    public void testConstructorSizeUnder8() throws Exception {
        ArrayEncodeBuffer localSut = new ArrayEncodeBuffer(7);
        assertThat(localSut.limitBytes(), is(8));

        localSut = new ArrayEncodeBuffer(8);
        assertThat(localSut.limitBytes(), is(8));
    }

    @Test
    public void testConstructorDefault() throws Exception {
        ArrayEncodeBuffer localSut = new ArrayEncodeBuffer();
        assertThat(localSut.limitBytes(), is(512));
    }

    @Test
    public void testWriteByte() throws Exception {
        sut_.writeByte(10);
        assertThat(sut_.toArray()[0], is((byte) 10));

        sut_.writeByte(20);
        assertThat(sut_.toArray()[1], is((byte) 20));
    }

    @Test
    public void testWriteBytesExpand() throws Exception {
        for (int i = 0; i < 9; i++) {
            sut_.writeByte(0);
        }
        assertThat(sut_.filledBytes(), is(9));
        assertThat(sut_.limitBytes(), is(16));
    }

    @Test
    public void testWriteBytesWhole() throws Exception {
        byte[] b = new byte[] {'0', '1', '2'};
        sut_.writeBytes(b, 0, 3);
        assertThat(sut_.filledBytes(), is(3));
        assertThat(sut_.toArray()[0], is((byte) 0x30));
        assertThat(sut_.toArray()[1], is((byte) 0x31));
        assertThat(sut_.toArray()[2], is((byte) 0x32));
    }

    @Test
    public void testWriteBytesWholeExpand() throws Exception {
        byte[] b = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        sut_.writeBytes(b, 0, 10);
        assertThat(sut_.filledBytes(), is(10));
        assertThat(sut_.toArray()[0], is((byte) 0x30));
        assertThat(sut_.toArray()[1], is((byte) 0x31));
        assertThat(sut_.toArray()[8], is((byte) 0x38));
        assertThat(sut_.toArray()[9], is((byte) 0x39));
    }

    @Test
    public void testWriteBytesHead() throws Exception {
        byte[] b = new byte[] {'0', '1', '2'};
        sut_.writeBytes(b, 0, 2);
        assertThat(sut_.filledBytes(), is(2));
        assertThat(sut_.toArray()[0], is((byte) 0x30));
        assertThat(sut_.toArray()[1], is((byte) 0x31));
    }

    @Test
    public void testWriteBytesHeadExpand() throws Exception {
        byte[] b = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        sut_.writeBytes(b, 0, 9);
        assertThat(sut_.filledBytes(), is(9));
        assertThat(sut_.toArray()[0], is((byte) 0x30));
        assertThat(sut_.toArray()[1], is((byte) 0x31));
        assertThat(sut_.toArray()[7], is((byte) 0x37));
        assertThat(sut_.toArray()[8], is((byte) 0x38));
    }

    @Test
    public void testWriteBytesTail() throws Exception {
        byte[] b = new byte[] {'0', '1', '2'};
        sut_.writeBytes(b, 1, 2);
        assertThat(sut_.filledBytes(), is(2));
        assertThat(sut_.toArray()[0], is((byte) 0x31));
        assertThat(sut_.toArray()[1], is((byte) 0x32));
    }

    @Test
    public void testWriteBytesTailExpand() throws Exception {
        byte[] b = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        sut_.writeBytes(b, 1, 9);
        assertThat(sut_.filledBytes(), is(9));
        assertThat(sut_.toArray()[0], is((byte) 0x31));
        assertThat(sut_.toArray()[1], is((byte) 0x32));
        assertThat(sut_.toArray()[7], is((byte) 0x38));
        assertThat(sut_.toArray()[8], is((byte) 0x39));
    }

    @Test
    public void testWriteBytesByteBuffer() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{'0', '1', '2'});
        sut_.writeBytes(bb);
        assertThat(sut_.filledBytes(), is(3));
        assertThat(sut_.toArray()[0], is((byte) 0x30));
        assertThat(sut_.toArray()[1], is((byte) 0x31));
        assertThat(sut_.toArray()[2], is((byte) 0x32));
    }

    @Test
    public void testWriteBytesByteBufferExpand() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
        sut_.writeBytes(bb);
        assertThat(sut_.filledBytes(), is(10));
        assertThat(sut_.toArray()[0], is((byte) 0x30));
        assertThat(sut_.toArray()[1], is((byte) 0x31));
        assertThat(sut_.toArray()[8], is((byte) 0x38));
        assertThat(sut_.toArray()[9], is((byte) 0x39));
    }

    @Test
    public void testWriteBytes4() throws Exception {
        sut_.writeBytes4(-1, 0);
        assertThat(sut_.filledBytes(), is(0));

        sut_.writeBytes4(-1, 2);
        assertThat(sut_.filledBytes(), is(2));
        assertThat(sut_.toArray()[0], is((byte) 0xFF));
        assertThat(sut_.toArray()[1], is((byte) 0xFF));

        sut_.clear();
        sut_.writeBytes4(-1, 4);
        assertThat(sut_.filledBytes(), is(4));
        assertThat(sut_.toArray()[0], is((byte) 0xFF));
        assertThat(sut_.toArray()[1], is((byte) 0xFF));
        assertThat(sut_.toArray()[2], is((byte) 0xFF));
        assertThat(sut_.toArray()[3], is((byte) 0xFF));
    }

    @Test
    public void testWriteBytes4UnderRange() throws Exception {
        exceptionRule_.expect(IllegalArgumentException.class);
        sut_.writeBytes4(-1, -1);
    }

    @Test
    public void testWriteBytes4OverRange() throws Exception {
        exceptionRule_.expect(IllegalArgumentException.class);
        sut_.writeBytes4(-1, 5);
    }

    @Test
    public void testWriteBytes8() throws Exception {
        sut_.writeBytes8(-1, 0);
        assertThat(sut_.filledBytes(), is(0));

        sut_.writeBytes8(-1, 2);
        assertThat(sut_.filledBytes(), is(2));
        assertThat(sut_.toArray()[0], is((byte) 0xFF));
        assertThat(sut_.toArray()[1], is((byte) 0xFF));

        sut_.clear();
        sut_.writeBytes8(-1, 8);
        assertThat(sut_.filledBytes(), is(8));
        assertThat(sut_.toArray()[0], is((byte) 0xFF));
        assertThat(sut_.toArray()[1], is((byte) 0xFF));
        assertThat(sut_.toArray()[2], is((byte) 0xFF));
        assertThat(sut_.toArray()[3], is((byte) 0xFF));
        assertThat(sut_.toArray()[4], is((byte) 0xFF));
        assertThat(sut_.toArray()[5], is((byte) 0xFF));
        assertThat(sut_.toArray()[6], is((byte) 0xFF));
        assertThat(sut_.toArray()[7], is((byte) 0xFF));
    }

    @Test
    public void testWriteBytes8UnderRange() throws Exception {
        exceptionRule_.expect(IllegalArgumentException.class);
        sut_.writeBytes8(-1, -1);
    }

    @Test
    public void testWriteBytes8OverRange() throws Exception {
        exceptionRule_.expect(IllegalArgumentException.class);
        sut_.writeBytes4(-1, 9);
    }

    @Test
    public void testWriteShort() throws Exception {
        sut_.writeShort(Short.MAX_VALUE);
        byte[] b = sut_.toArray();
        assertThat(b[0], is((byte) 0x7F));
        assertThat(b[1], is((byte) 0xFF));
        assertThat(sut_.filledBytes(), is(2));
    }

    @Test
    public void testWriteChar() throws Exception {
        sut_.writeChar(Character.MAX_VALUE);
        byte[] b = sut_.toArray();
        assertThat(b[0], is((byte) 0xFF));
        assertThat(b[1], is((byte) 0xFF));
        assertThat(sut_.filledBytes(), is(2));
    }

    @Test
    public void testWriteInt() throws Exception {
        sut_.writeInt(Integer.MIN_VALUE);
        byte[] b = sut_.toArray();
        assertThat(b[0], is((byte) 0x80));
        assertThat(b[1], is((byte) 0x00));
        assertThat(b[2], is((byte) 0x00));
        assertThat(b[3], is((byte) 0x00));
        assertThat(sut_.filledBytes(), is(4));
    }

    @Test
    public void testWriteLong() throws Exception {
        sut_.writeLong(1);
        byte[] b = sut_.toArray();
        assertThat(b[0], is((byte) 0x00));
        assertThat(b[1], is((byte) 0x00));
        assertThat(b[2], is((byte) 0x00));
        assertThat(b[3], is((byte) 0x00));
        assertThat(b[4], is((byte) 0x00));
        assertThat(b[5], is((byte) 0x00));
        assertThat(b[6], is((byte) 0x00));
        assertThat(b[7], is((byte) 0x01));
        assertThat(sut_.filledBytes(), is(8));
    }

    @Test
    public void testWriteFloat() throws Exception {
        sut_.writeFloat(1f);
        byte[] b = sut_.toArray();
        assertThat(b[0], is((byte) 0x3F));
        assertThat(b[1], is((byte) 0x80));
        assertThat(b[2], is((byte) 0x00));
        assertThat(b[3], is((byte) 0x00));
        assertThat(sut_.filledBytes(), is(4));
    }

    @Test
    public void testWriteDouble() throws Exception {
        sut_.writeDouble(1d);
        byte[] b = sut_.toArray();
        assertThat(b[0], is((byte) 0x3F));
        assertThat(b[1], is((byte) 0xF0));
        assertThat(b[2], is((byte) 0x00));
        assertThat(b[3], is((byte) 0x00));
        assertThat(b[4], is((byte) 0x00));
        assertThat(b[5], is((byte) 0x00));
        assertThat(b[6], is((byte) 0x00));
        assertThat(b[7], is((byte) 0x00));
        assertThat(sut_.filledBytes(), is(8));
    }

    @Test
    public void testWriteString() throws Exception {
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        sut_.writeString(encoder, "abc");
        byte[] b = sut_.toArray();
        assertThat(b[0], is((byte) (CodecUtil.VB_END_BIT | 3)));
        assertThat(b[1], is((byte) 'a'));
        assertThat(b[2], is((byte) 'b'));
        assertThat(b[3], is((byte) 'c'));
    }

    @Test
    public void testWriteStringOverflow() throws Exception {
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        String data = "01234567890123456789あいうえおかきくけこあいうえおかきくけこあいうえお";
        byte[] dataUTF8 = data.getBytes(StandardCharsets.UTF_8);

        // expand internal buffer and byte length estimation failure happen.
        sut_.writeString(encoder, data);

        byte[] b = sut_.toArray();
        assertThat(Arrays.copyOf(b, 2), is(new byte[]{0x1F, (byte) 0x81})); // dataUTF8.length
        for (int i = 0; i < dataUTF8.length; i++) {
            assertThat("index:" + i, b[i + 2], is(dataUTF8[i]));
        }
    }

    @Test
    public void testDrainTo() throws Exception {
        sut_.writeInt(0x01020304);
        sut_.writeInt(0x05060708);
        ArrayEncodeBuffer b = new ArrayEncodeBuffer(8);

        sut_.drainTo(b);

        assertThat(sut_.filledBytes(), is(0));
        assertThat(b.filledBytes(), is(8));
        assertThat(Arrays.copyOf(b.toArray(), 8), is(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}));
        assertThat(b.limitBytes(), is(16));
    }

    @Test
    public void testToArray() throws Exception {
        assertThat(sut_.hasArray(), is(true));
        assertThat(sut_.arrayOffset(), is(0));

        byte[] b = sut_.toArray();
        assertThat(b[0], is((byte) 0));

        sut_.writeByte('a');
        assertThat(b[0], is((byte) 'a'));
    }
}
