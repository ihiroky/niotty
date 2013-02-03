package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;

/**
 * Created on 13/02/01, 15:22
 *
 * @author Hiroki Itoh
 */
public class ArrayDecodeBuffer implements DecodeBuffer {

    private byte[][] banks;
    private int bankIndex;
    private int countInBank;
    private int lastBankIndex;
    private int lastCountInBank;
    private final int bankLength;

    private static final int DEFAULT_BANK_LENGTH = 1024;
    private static final int BANKS_PER_GROW = 8;

    ArrayDecodeBuffer() {
        this(DEFAULT_BANK_LENGTH);
    }

    ArrayDecodeBuffer(int bankLength) {
        this.banks = new byte[1][];
        this.bankLength = bankLength;
    }

    private void proceedBankIfInBankLast() {
        if (countInBank == bankLength) {
            bankIndex++;
            countInBank = 0;
        }
    }

    @Override
    public int readByte() {
        int b = banks[bankIndex][countInBank++] & CodecUtil.BYTE_MASK;
        proceedBankIfInBankLast();
        return b;
    }

    @Override
    public int readBytes(byte[] bytes, int offset, int length) {
        int bi = bankIndex;
        int cib = countInBank;
        int left = bankLength - cib;
        if (length <= left) {
            System.arraycopy(banks[bi], cib, bytes, offset, length);
            countInBank += length;
            proceedBankIfInBankLast();
            return length;
        }

        int nbi = bi + 1;
        int nc = length - left;
        System.arraycopy(banks[bi], cib, bytes, offset, left);
        System.arraycopy(banks[nbi], 0, bytes, offset + left, nc);
        bankIndex = nbi;
        countInBank = nc;
        return length;
    }

    @Override
    public int readBytes4(int bytes) {
        return (int) readBytes8(bytes);
    }

    @Override
    public long readBytes8(int bytes) {
        int cib = countInBank;
        int left = bankLength - cib;
        if (bytes <= left) {
            byte[] bank = banks[bankIndex];
            long decoded = 0L;
            for (int i = 0; i < bytes; i++) {
                decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (bank[cib + i] & CodecUtil.BYTE_MASK);
            }
            proceedBankIfInBankLast();
            return decoded;
        }
        return readBytes8AcrossBanks(bytes, left);
    }

    // FROM HERE
    private long readBytes8AcrossBanks(int bytes, int left) {
        int bi = bankIndex;
        int cib = countInBank;
        byte[] bank = banks[bi];
        long decoded = 0L;
        int i = 0;
        for (; i < left; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (bank[cib + i] & CodecUtil.BYTE_MASK);
        }
        bank = banks[++bi];
        for (; i< bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (bank[i - left] & CodecUtil.BYTE_MASK);
        }
        bankIndex = bi;
        countInBank = bytes - left;
        return decoded;
    }

    @Override
    public char readChar() {
        int cib = countInBank;
        int left = bankLength - cib;
        if (left >= CodecUtil.CHAR_BYTES) {
            byte[] bank = banks[bankIndex];
            char decoded = (char) (((bank[cib] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                    | (bank[cib + 1] & CodecUtil.BYTE_MASK));
            proceedBankIfInBankLast();
            return decoded;
        }
        int bi = bankIndex;
        char decoded = (char) ((banks[bi][cib] & CodecUtil.BYTE_MASK) | (banks[bi + 1][0] & CodecUtil.BYTE_MASK));
        bankIndex = bi + 1;
        countInBank = 1;
        return decoded;

    }

    @Override
    public short readShort() {
        int cib = countInBank;
        int left = bankLength - cib;
        if (left >= CodecUtil.SHORT_BYTES) {
            byte[] bank = banks[bankIndex];
            short decoded = (short) (((bank[cib] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                    | (bank[cib + 1] & CodecUtil.BYTE_MASK));
            proceedBankIfInBankLast();
            return decoded;
        }
        int bi = bankIndex;
        short decoded = (short) ((banks[bi][cib] & CodecUtil.BYTE_MASK) | (banks[bi + 1][0] & CodecUtil.BYTE_MASK));
        bankIndex = bi + 1;
        countInBank = 1;
        return decoded;
    }

    @Override
    public int readInt() {
        int cib = countInBank;
        int left = bankLength - cib;
        if (left >= CodecUtil.INT_BYTES) {
            byte[] bank = banks[bankIndex];
            int decoded = ((bank[cib] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT3)
                    | ((bank[cib + 1] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                    | ((bank[cib + 2] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                    | (bank[cib + 3] & CodecUtil.BYTE_MASK);
            proceedBankIfInBankLast();
            return decoded;
        }
        return (int) readBytes8AcrossBanks(CodecUtil.INT_BYTES, left);
    }

    @Override
    public long readLong() {
        int cib = countInBank;
        int left = bankLength - cib;
        if (left >= CodecUtil.LONG_BYTES) {
            byte[] bank = banks[bankIndex];
            long decoded = (((long) bank[cib] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT7)
                    | (((long) bank[cib + 1] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT6)
                    | (((long) bank[cib + 2] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT5)
                    | (((long) bank[cib + 3] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT4)
                    | (((long) bank[cib + 4] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT3)
                    | (((long) bank[cib + 5] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                    | (((long) bank[cib + 6] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                    | ((long) bank[cib + 7] & CodecUtil.BYTE_MASK);
            proceedBankIfInBankLast();
            return decoded;
        }
        return readBytes8AcrossBanks(CodecUtil.LONG_BYTES, left);
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public void clear() {
        bankIndex = 0;
        countInBank = 0;
    }

    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public int leftBytes() {
        return bankLength * (lastBankIndex - bankIndex) + lastCountInBank - countInBank;
    }

    @Override
    public int wholeBytes() {
        return bankLength * lastBankIndex + lastCountInBank;
    }

    @Override
    public BufferSink toBufferSink() {
        return new ArrayBufferSink(banks);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        if (lastBankIndex == 0) {
            return ByteBuffer.wrap(banks[0], 0, lastCountInBank);
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(wholeBytes());
        for (int i = 0; i < lastBankIndex; i++) {
            byteBuffer.put(banks[i], 0, bankLength);
        }
        byteBuffer.put(banks[lastBankIndex], 0, lastCountInBank);
        return byteBuffer;
    }

    // TODO FileChannel direct transfer ?
    @Override
    public void transferFrom(ByteBuffer byteBuffer) {
        byte[][] bs = banks;
        int bi = lastBankIndex;
        int space = bankLength - lastCountInBank;
        int read;
        int remaining = 0;
        while (byteBuffer.hasRemaining()) {
            if (bi == bs.length) {
                byte[][] t = new byte[bs.length + BANKS_PER_GROW][];
                System.arraycopy(bs, 0, t, 0, bs.length);
                bs = t;
            }
            if (space == 0) {
                bs[++bi] = new byte[bankLength];
                space = bankLength;
            }
            remaining = byteBuffer.remaining();
            read = (remaining >= space) ? space : remaining;
            byteBuffer.get(bs[bi], bankLength - space, space);
            space -= read;
        }
        banks = bs;
        lastBankIndex = bi;
        lastCountInBank = remaining;
    }
}
