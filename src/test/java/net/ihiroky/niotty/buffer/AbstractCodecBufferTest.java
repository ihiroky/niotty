package net.ihiroky.niotty.buffer;

import net.ihiroky.niotty.util.Charsets;
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
public class AbstractCodecBufferTest {

    private ArrayCodecBuffer sut_;

    static AbstractCodecBuffer newInstance(byte[] data, int offset, int length) {
        return new ArrayCodecBuffer(data, offset, length);
    }

    @Rule
    public ExpectedException thrownRule_ = ExpectedException.none();

    @Before
    public void setUp() {
        sut_ = new ArrayCodecBuffer(ArrayChunkFactory.instance(), 0);
    }

    @Test
    public void testWriteVariableByteNull() throws Exception {
        sut_.writeVariableByteNull();
        assertThat(sut_.array()[0], is((byte) 0xc0));
        assertThat(sut_.remaining(), is(1));
    }

    @Test
    public void testWriteVariableByteInteger0() throws Exception {
        sut_.writeVariableByteInteger(0);
        assertThat(sut_.array()[0], is((byte) 0x80));
        assertThat(sut_.remaining(), is(1));
    }

    @Test
    public void testWriteVariableByteLong0() throws Exception {
        sut_.writeVariableByteLong(0);
        assertThat(sut_.array()[0], is((byte) 0x80));
        assertThat(sut_.remaining(), is(1));
    }

    @Test
    public void testWriteVariableByte1() throws Exception {
        sut_.writeVariableByteInteger(1);
        assertThat(sut_.array()[0], is((byte) 0x81));
        assertThat(sut_.remaining(), is(1));
    }

    @Test
    public void testWriteVariableByteLong1() throws Exception {
        sut_.writeVariableByteLong(1);
        assertThat(sut_.array()[0], is((byte) 0x81));
        assertThat(sut_.remaining(), is(1));
    }

    @Test
    public void testWriteVariableByteIntegerMinValue() throws Exception {
        sut_.writeVariableByteInteger(Integer.MIN_VALUE);
        assertThat(Arrays.copyOf(sut_.array(), 5), is(new byte[] {
                0x40, 0x00, 0x00, 0x00, (byte) 0x90,
        }));
        assertThat(sut_.remaining(), is(5));
    }

    @Test
    public void testWriteVariableByteLongMinValue() throws Exception {
        sut_.writeVariableByteLong(Long.MIN_VALUE);
        assertThat(Arrays.copyOf(sut_.array(), 10), is(new byte[] {
                0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x82,
        }));
        assertThat(sut_.remaining(), is(10));
    }

    @Test
    public void testWriteVariableByteInteger229() throws Exception {
        sut_.writeVariableByteInteger(229);
        assertThat(Arrays.copyOf(sut_.array(), 2), is(new byte[]{0x25, (byte) 0x83}));
        assertThat(sut_.remaining(), is(2));
    }

    @Test
    public void testWriteVariableByteLong229() throws Exception {
        sut_.writeVariableByteLong(229);
        assertThat(Arrays.copyOf(sut_.array(), 2), is(new byte[]{0x25, (byte) 0x83}));
        assertThat(sut_.remaining(), is(2));
    }

    @Test
    public void testWriteVariableByteIntegerMinus229() throws Exception {
        sut_.writeVariableByteInteger(-229);
        assertThat(Arrays.copyOf(sut_.array(), 2), is(new byte[]{0x65, (byte) 0x83}));
        assertThat(sut_.remaining(), is(2));
    }

    @Test
    public void testWriteVariableByteLongMinus229() throws Exception {
        sut_.writeVariableByteLong(-229);
        assertThat(Arrays.copyOf(sut_.array(), 2), is(new byte[]{0x65, (byte) 0x83}));
        assertThat(sut_.remaining(), is(2));
    }

    @Test
    public void testWriteVariableByte_LongCast() throws Exception {
        sut_.writeVariableByteLong(Long.MAX_VALUE);
        assertThat(Arrays.copyOf(sut_.array(), 10),
                is(new byte[]{0x3F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, (byte) 0x81}));
        assertThat(sut_.remaining(), is(10));
    }

    @Test
    public void testWriteVariableByteIntegerObj() throws Exception {
        sut_.writeVariableByteInteger(Integer.valueOf(1));
        assertThat(sut_.array()[0], is((byte) 0x81));
        assertThat(sut_.remaining(), is(1));
    }

    @Test
    public void testWriteVariableByteIntegerNull() throws Exception {
        sut_.writeVariableByteInteger(null);
        assertThat(sut_.array()[0], is((byte) 0xc0));
        assertThat(sut_.remaining(), is(1));
    }

    @Test
    public void testWriteVariableByteLongObj() throws Exception {
        sut_.writeVariableByteLong(Long.valueOf(1L));
        assertThat(sut_.array()[0], is((byte) 0x81));
        assertThat(sut_.remaining(), is(1));
    }

    @Test
    public void testWriteVariableByteLongNull() throws Exception {
        sut_.writeVariableByteLong(null);
        assertThat(sut_.array()[0], is((byte) 0xc0));
        assertThat(sut_.remaining(), is(1));
    }

    @Test
    public void testReadVariableByteNumber0() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{(byte) 0x80}, 0, 1);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual.intValue(), is(0));
    }

    @Test
    public void testReadVariableByteNumber1() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{(byte) 0x81}, 0, 1);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual.intValue(), is(1));
    }

    @Test
    public void testReadVariableByteNumberMinus1() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{(byte) 0xc1}, 0, 1);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual.intValue(), is(-1));
    }

    @Test
    public void testReadVariableByteNumber229() throws Exception {
        byte[] data = new byte[]{0x25, (byte) 0x83};
        AbstractCodecBuffer sut = newInstance(data, 0, data.length);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual.intValue(), is(0xE5)); // 0b11100101
    }

    @Test
    public void testReadVariableByteNumberMinus229() throws Exception {
        byte[] data = new byte[]{0x65, (byte) 0x83};
        AbstractCodecBuffer sut = newInstance(data, 0, data.length);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual.intValue(), is(-0xE5)); // 0b11100101
    }

    @Test
    public void testReadVariableByteNumberNull() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{(byte) 0xc0}, 0, 1);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testReadVariableByteNumberMinValue() throws Exception {
        byte[] data = new byte[] {
                0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x82
        };
        AbstractCodecBuffer sut = newInstance(data, 0, data.length);
        Number actual = sut.readVariableByteNumber();
        assertThat(actual.longValue(), is(Long.MIN_VALUE));
    }

    @Test
    public void testReadVariableByteNumber_32bitLong() throws Exception {
        long expected = Integer.MAX_VALUE + 1L;
        sut_.writeVariableByteLong(expected);
        Number actual = sut_.readVariableByteNumber();
        assertThat(actual, is((Number) expected));
    }

    @Test
    public void testReadVariableByteLong0() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{(byte) 0x80}, 0, 1);
        long actual = sut.readVariableByteLong();
        assertThat(actual, is(0L));
    }

    @Test
    public void testReadVariableByteLong1() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{(byte) 0x81}, 0, 1);
        long actual = sut.readVariableByteLong();
        assertThat(actual, is(1L));
    }

    @Test
    public void testReadVariableByteLongMinus0() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{(byte) 0xc0}, 0, 1);
        long actual = sut.readVariableByteLong();
        assertThat(actual, is(0L));
    }

    @Test
    public void testReadVariableByteLongMinus1() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{(byte) 0xc1}, 0, 1);
        long actual = sut.readVariableByteLong();
        assertThat(actual, is(-1L));
    }

    @Test
    public void testReadVariableByte_LongCast() throws Exception {
        AbstractCodecBuffer sut = newInstance(
                new byte[]{0x3F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, (byte) 0x81}, 0, 10);
        long actual = sut.readVariableByteLong();
        assertThat(actual, is(Long.MAX_VALUE));
    }

    @Test
    public void testReadVariableByteLong_1024() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{0x00, (byte) 0x90}, 0, 2);
        long actual = sut.readVariableByteLong();
        assertThat(actual, is(1024L));
    }

    @Test
    public void testReadVariableByteLongMinValue() throws Exception {
        byte[] data = new byte[] {
                0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x82,
        };
        AbstractCodecBuffer sut = newInstance(data, 0, data.length);
        long actual = sut.readVariableByteLong();
        assertThat(actual, is(Long.MIN_VALUE));
    }

    @Test
    public void testReadVariableByteInteger0() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{(byte) 0x80}, 0, 1);
        int actual = sut.readVariableByteInteger();
        assertThat(actual, is(0));
    }

    @Test
    public void testReadVariableByteInteger1() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{(byte) 0x81}, 0, 1);
        int actual = sut.readVariableByteInteger();
        assertThat(actual, is(1));
    }

    @Test
    public void testReadVariableByteIntegerMinus0() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{(byte) 0xc0}, 0, 1);
        int actual = sut.readVariableByteInteger();
        assertThat(actual, is(0));
    }

    @Test
    public void testReadVariableByteIntegerMinus1() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{(byte) 0xc1}, 0, 1);
        int actual = sut.readVariableByteInteger();
        assertThat(actual, is(-1));
    }

    @Test
    public void testReadVariableByteInteger_1024() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[]{0x00, (byte) 0x90}, 0, 2);
        int actual = sut.readVariableByteInteger();
        assertThat(actual, is(1024));
    }

    @Test
    public void testReadVariableByteIntegerMinValue() throws Exception {
        byte[] data = new byte[] {
                0x40, 0x00, 0x00, 0x00, (byte) 0x90,
        };
        AbstractCodecBuffer sut = newInstance(data, 0, data.length);
        int actual = sut.readVariableByteInteger();
        assertThat(actual, is(Integer.MIN_VALUE));
    }

    @Test
    public void testWriteLongAsAsciiMinValue() throws Exception {
        String expected = Long.toString(Long.MIN_VALUE);
        AbstractCodecBuffer sut = newInstance(new byte[expected.length()], 0, 0);

        sut.writeLongAsAscii(Long.MIN_VALUE);

        String actual = sut.readStringContent(Charsets.US_ASCII.newDecoder(), expected.length());
        assertThat(actual, is(expected));
        assertThat(sut.remaining(), is(0));
    }

    @Test
    public void testWriteLongAsAsciiMoreThanIntMax() throws Exception {
        String expected = Long.toString(Integer.MAX_VALUE + 1L);
        AbstractCodecBuffer sut = newInstance(new byte[expected.length()], 0, 0);

        sut.writeLongAsAscii(Integer.MAX_VALUE + 1L);

        String actual = sut.readStringContent(Charsets.US_ASCII.newDecoder(), expected.length());
        assertThat(actual, is(expected));
        assertThat(sut.remaining(), is(0));
    }

    @Test
    public void testWriteLongAsAsciiMoreThan65535() throws Exception {
        String expected = Long.toString(65536);
        AbstractCodecBuffer sut = newInstance(new byte[expected.length()], 0, 0);

        sut.writeLongAsAscii(65536);

        String actual = sut.readStringContent(Charsets.US_ASCII.newDecoder(), expected.length());
        assertThat(actual, is(expected));
        assertThat(sut.remaining(), is(0));
    }

    @Test
    public void testWriteLongAsAsciiLessThan65536() throws Exception {
        String expected = Long.toString(65535);
        AbstractCodecBuffer sut = newInstance(new byte[expected.length()], 0, 0);

        sut.writeLongAsAscii(65535);

        String actual = sut.readStringContent(Charsets.US_ASCII.newDecoder(), expected.length());
        assertThat(actual, is(expected));
        assertThat(sut.remaining(), is(0));
    }

    @Test
    public void testWriteLongAsAsciiNegative() throws Exception {
        String expected = Long.toString(Integer.MIN_VALUE - 1L);
        AbstractCodecBuffer sut = newInstance(new byte[expected.length()], 0, 0);

        sut.writeLongAsAscii(Integer.MIN_VALUE - 1L);

        String actual = sut.readStringContent(Charsets.US_ASCII.newDecoder(), expected.length());
        assertThat(actual, is(expected));
        assertThat(sut.remaining(), is(0));
    }

    @Test
    public void testReadLongAsAsciiPositive() throws Exception {
        long expected = Integer.MAX_VALUE + 1L;
        String expectedString = Long.toString(expected);
        AbstractCodecBuffer sut = newInstance(new byte[expectedString.length()], 0, 0);
        sut.writeLongAsAscii(expected);

        long actual = sut.readLongAsAscii(expectedString.length());

        assertThat(actual, is(expected));
    }

    @Test
    public void testReadLongAsAsciiNegative() throws Exception {
        long expected = Integer.MIN_VALUE - 1L;
        String expectedString = Long.toString(expected);
        AbstractCodecBuffer sut = newInstance(new byte[expectedString.length()], 0, 0);
        sut.writeLongAsAscii(expected);

        long actual = sut.readLongAsAscii(expectedString.length());

        assertThat(actual, is(expected));
    }

    @Test
    public void testReadLongAsAsciiLongMinValue() throws Exception {
        long expected = Long.MIN_VALUE;
        String expectedString = Long.toString(expected);
        AbstractCodecBuffer sut = newInstance(new byte[expectedString.length()], 0, 0);
        sut.writeLongAsAscii(expected);

        long actual = sut.readLongAsAscii(expectedString.length());

        assertThat(actual, is(expected));
    }

    @Test
    public void testReadLongAsAsciiOverLongMaxValue() throws Exception {
        String overLongMaxValue = Long.toString(Long.MIN_VALUE).substring(1);
        AbstractCodecBuffer sut = newInstance(new byte[overLongMaxValue.length()], 0, 0);
        sut.writeStringContent(overLongMaxValue, Charsets.US_ASCII.newEncoder());

        thrownRule_.expect(NumberFormatException.class);
        thrownRule_.expectMessage(overLongMaxValue);

        sut.readLongAsAscii(overLongMaxValue.length());
    }

    @Test
    public void testReadLongAsAsciiOverLongMaxValueTwentyDigits() throws Exception {
        String overLongMaxValue = "99999999999999999999";
        AbstractCodecBuffer sut = newInstance(new byte[overLongMaxValue.length()], 0, 0);
        sut.writeStringContent(overLongMaxValue, Charsets.US_ASCII.newEncoder());

        thrownRule_.expect(NumberFormatException.class);
        thrownRule_.expectMessage(overLongMaxValue);

        sut.readLongAsAscii(overLongMaxValue.length());
    }

    @Test
    public void testReadLongAsAsciiUnderLongMinValue() throws Exception {
        String underLongMinValue = "-9223372036854775809";
        AbstractCodecBuffer sut = newInstance(new byte[underLongMinValue.length()], 0, 0);
        sut.writeStringContent(underLongMinValue, Charsets.US_ASCII.newEncoder());

        thrownRule_.expect(NumberFormatException.class);
        thrownRule_.expectMessage(underLongMinValue);

        sut.readLongAsAscii(underLongMinValue.length());
    }

    @Test
    public void testReadLongAsAsciiUnderLongMaxValueNineteenDigits() throws Exception {
        String overLongMaxValue = "-9999999999999999999";
        AbstractCodecBuffer sut = newInstance(new byte[overLongMaxValue.length()], 0, 0);
        sut.writeStringContent(overLongMaxValue, Charsets.US_ASCII.newEncoder());

        thrownRule_.expect(NumberFormatException.class);
        thrownRule_.expectMessage(overLongMaxValue);

        sut.readLongAsAscii(overLongMaxValue.length());
    }

    @Test
    public void testReadLongAsAsciiTheContentIsShort() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[0], 0, 0);

        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("Invalid length: 1, remaining: 0");

        sut.readLongAsAscii(1);
    }

    @Test
    public void testReadLongAsAsciiMoreThanTwentyDigits() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[21], 0, 0);

        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("The length must be less than or equal 20");

        sut.readLongAsAscii(21);
    }

    @Test
    public void testReadLongAsAsciiZeroLength() throws Exception {
        AbstractCodecBuffer sut = newInstance(new byte[0], 0, 0);

        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("Invalid length: 0, remaining: 0");

        sut.readLongAsAscii(0);
    }
}
