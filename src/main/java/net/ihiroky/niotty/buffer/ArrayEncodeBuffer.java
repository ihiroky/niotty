package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Queue;

/**
 * A buffer to encode primitive and byte array values.
 * A byte array to store encoded bytes increase its capacity as needed.
 * The byte order is little endian.
 *
 * @author Hiroki Itoh
 */
public class ArrayEncodeBuffer implements EncodeBuffer, BufferSink {

    private byte[][] banks;
    private int bankIndex;
    private int countInBank;
    private final int bankLength;

    private static final int DEFAULT_BANK_LENGTH = 1024;

    public ArrayEncodeBuffer() {
        this(DEFAULT_BANK_LENGTH);
    }

    public ArrayEncodeBuffer(int bankLength) {
        if (bankLength < 16) {
            throw new IllegalArgumentException("bankLength must be ge 16");
        }

        byte[][] banks = new byte[1][];
        banks[0] = new byte[bankLength];
        this.banks = banks;
        this.bankLength = bankLength;
    }

    private void proceedBankIfInBankLast() {
        if (countInBank == bankLength) {
            bankIndex++;
            countInBank = 0;
        }
    }

    @Override
    public void writeByte(int value) {
        ensureSpace(1);
        banks[bankIndex][countInBank++] = (byte) value;
        proceedBankIfInBankLast();
    }

    @Override
    public void writeBytes(byte[] bytes, int offset, int length) {
        int bi = bankIndex;
        int cib = countInBank;
        int space = bankLength - cib;
        if (space >= length) {
            System.arraycopy(bytes, offset, banks[bi], cib, length);
            countInBank += length;
            proceedBankIfInBankLast();
            return;
        }

        // write to current and next bank
        ensureSpace(length);
        System.arraycopy(bytes, offset, banks[bi++], cib, space);
        int leftOffset = offset + space;
        int leftBytes = length - space;
        space = bankLength;
        while (leftBytes > space) {
            System.arraycopy(bytes, leftOffset, banks[bi++], 0, space);
            leftOffset += space;
            leftBytes -= space;
        }
        System.arraycopy(bytes, leftOffset, banks[bi], 0, leftBytes);
        bankIndex = bi;
        countInBank = leftBytes;
    }

    @Override
    public void writeBytes4(int bits, int bytes) {
        writeBytes8(bits & 0xFFFFFFFFL, bytes);
    }

    @Override
    public void writeBytes8(long bits, int bytes) {
        int cib = countInBank;
        int space = bankLength - cib;
        if (space >= bytes) {
            byte[] bank = banks[bankIndex];
            int bytesMinus1 = bytes - 1;
            for (int i = 0; i < bytes; i++) {
                bank[cib + i] = (byte) ((bits >>> (bytesMinus1 - i)) & CodecUtil.BYTE_MASK);
            }
            countInBank += bytes;
            proceedBankIfInBankLast();
            return;
        }
        writeBytes8AcrossBanks(bits, bytes, space);
    }

    private void writeBytes8AcrossBanks(long bits, int bytes, int space) {
        ensureSpace(bytes);
        int bi = bankIndex;
        int cib = countInBank;
        int bytesMinus1 = bytes - 1;
        byte[] bank = banks[bi];
        int i = 0;
        for (; i < space; i++) {
            bank[cib + i] = (byte) ((bits >>> (bytesMinus1 - i)) & CodecUtil.BYTE_MASK);
        }
        bank = banks[++bi];
        for (; i< bytes; i++) {
            bank[i - space] = (byte) ((bits >>> (bytesMinus1 - i)) & CodecUtil.BYTE_MASK);
        }
        bankIndex = bi;
        countInBank = bytes - space;
    }

    @Override
    public void writeChar(char value) {
        int bi = bankIndex;
        int cib = countInBank;
        int space = bankLength - cib;
        if (space >= CodecUtil.SHORT_BYTES) {
            byte[] bank = banks[bi];
            bank[cib    ] = (byte) (value >>> CodecUtil.BYTE_SHIFT1);
            bank[cib + 1] = (byte) (value & CodecUtil.BYTE_MASK);
            proceedBankIfInBankLast();
            return;
        }

        ensureSpace(CodecUtil.CHAR_BYTES);
        banks[bi][cib] = (byte) (value >>> CodecUtil.BYTE_SHIFT1);
        banks[bi + 1][0] = (byte) (value & CodecUtil.BYTE_MASK);
        bankIndex = bi + 1;
        countInBank = 1;
    }

    @Override
    public void writeShort(short value) {
        int bi = bankIndex;
        int cib = countInBank;
        int space = bankLength - cib;
        if (space >= CodecUtil.SHORT_BYTES) {
            byte[] bank = banks[bi];
            bank[cib    ] = (byte) (value >>> CodecUtil.BYTE_SHIFT1);
            bank[cib + 1] = (byte) (value & CodecUtil.BYTE_MASK);
            proceedBankIfInBankLast();
            return;
        }

        ensureSpace(CodecUtil.SHORT_BYTES);
        banks[bi][cib] = (byte) (value >>> CodecUtil.BYTE_SHIFT1);
        banks[bi + 1][0] = (byte) (value & CodecUtil.BYTE_MASK);
        bankIndex = bi + 1;
        countInBank = 1;
    }

    @Override
    public void writeInt(int value) {
        int cib = countInBank;
        int space = bankLength - cib;
        if (space >= CodecUtil.INT_BYTES) {
            byte[] bank = banks[bankIndex];
            bank[cib    ] = (byte) (value >>> CodecUtil.BYTE_SHIFT3);
            bank[cib + 1] = (byte) ((value >>> CodecUtil.BYTE_SHIFT2) & CodecUtil.BYTE_MASK);
            bank[cib + 2] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
            bank[cib + 3] = (byte) (value & CodecUtil.BYTE_MASK);
            proceedBankIfInBankLast();
            return;
        }
        writeBytes8AcrossBanks(value & 0xFFFFFFFFL, CodecUtil.INT_BYTES, space);
    }

    @Override
    public void writeLong(long value) {
        int cib = countInBank;
        int space = bankLength - cib;
        if (space >= CodecUtil.INT_BYTES) {
            byte[] bank = banks[bankIndex];
            bank[cib    ] = (byte) (value >>> CodecUtil.BYTE_SHIFT7);
            bank[cib + 1] = (byte) ((value >>> CodecUtil.BYTE_SHIFT6) & CodecUtil.BYTE_MASK);
            bank[cib + 2] = (byte) ((value >>> CodecUtil.BYTE_SHIFT5) & CodecUtil.BYTE_MASK);
            bank[cib + 3] = (byte) ((value >>> CodecUtil.BYTE_SHIFT4) & CodecUtil.BYTE_MASK);
            bank[cib + 4] = (byte) ((value >>> CodecUtil.BYTE_SHIFT3) & CodecUtil.BYTE_MASK);
            bank[cib + 5] = (byte) ((value >>> CodecUtil.BYTE_SHIFT2) & CodecUtil.BYTE_MASK);
            bank[cib + 6] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
            bank[cib + 7] = (byte) (value & CodecUtil.BYTE_MASK);
            proceedBankIfInBankLast();
            return;
        }
        writeBytes8AcrossBanks(value, CodecUtil.LONG_BYTES, space);
    }

    @Override
    public void writeFloat(float value) {
        writeInt(Float.floatToIntBits(value));
    }

    @Override
    public void writeDouble(double value) {
        writeLong(Double.doubleToLongBits(value));
    }

    private void ensureSpace(int bytes) {
        int space = bankLength - countInBank;
        if (bytes <= space) {
            return;
        }
        int requiredBytes = bytes - space;
        for (int currentBankIndex = bankIndex; requiredBytes > 0; requiredBytes -= bankLength) {
            banks[++currentBankIndex] = new byte[bankLength];
        }
    }

    @Override
    public int filledBytes() {
        return bankIndex * bankLength + countInBank;

    }

    @Override
    public void clear() {
        bankIndex = 0;
        countInBank = 0;
    }

    @Override
    public boolean needsDirectTransfer() {
        return false;
    }

    @Override
    public void transferTo(ByteBuffer writeBuffer) {
        byte[][] bs = banks;
        int n = bs.length - 1;
        for (int i = 0; i < n; i++) {
            writeBuffer.put(bs[i]);
        }
        writeBuffer.put(bs[n], 0, countInBank);
    }

    @Override
    public void transferTo(Queue<ByteBuffer> writeQueue) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(filledBytes());
        transferTo(byteBuffer);
        writeQueue.offer(byteBuffer);
    }

    @Override
    public void transferTo(WritableByteChannel channel) {
    }
}
