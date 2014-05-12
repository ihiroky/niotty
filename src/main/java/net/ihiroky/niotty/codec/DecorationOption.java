package net.ihiroky.niotty.codec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.util.Calendar;

/**
 * Provides options to specify decorations for {@link net.ihiroky.niotty.buffer.Packet}.
 *
 * This implementation is not synchronized. So the synchronization
 * is required if multiple threads access this instance.
 */
public class DecorationOption {

    private final ByteBuffer timestampBuffer_;
    private final ByteBuffer packetSizeBuffer_;
    private final ByteBuffer separatorBuffer_;
    private final int size_;

    private transient Calendar calendar_;
    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

    /**
     * Constructs a new instance.
     *
     * @param addTimestamp true if the timestamp is added
     * @param addPacketSize true if the length of the packet is added
     * @param separator data appended after the packet
     */
    public DecorationOption(boolean addTimestamp, boolean addPacketSize, byte[] separator) {
        int size = 0;
        if (addTimestamp) {
            timestampBuffer_ = ByteBuffer.allocate(20); // "yyyyMMdd-HHmmss.SSS "
            size += timestampBuffer_.capacity();
        } else {
            timestampBuffer_ = null;
        }

        if (addPacketSize) {
            packetSizeBuffer_ = ByteBuffer.allocate(4);
            size += 4;
        } else {
            packetSizeBuffer_ = null;
        }

        if (separator != null) {
            separatorBuffer_ = ByteBuffer.wrap(separator);
            size += separator.length;
        } else {
            separatorBuffer_ = EMPTY;
        }

        size_ = size;
        calendar_ = Calendar.getInstance();
    }

    /**
     * Returns the size to be added by this decoration.
     * @return the size
     */
    public int size() {
        return size_;
    }

    /**
     * Prepends the timestamp and size if required.
     * @param channel the channel to be written into
     * @param size the size
     * @param now the time in milliseconds
     * @throws IOException
     */
    public void prepend(GatheringByteChannel channel, int size, long now) throws IOException {
        ByteBuffer timestampBuffer = timestampBuffer_;
        if (timestampBuffer != null) {
            Calendar c = calendar_;
            c.setTimeInMillis(now);
            append0000(timestampBuffer, c.get(Calendar.YEAR));
            append00(timestampBuffer, c.get(Calendar.MONTH) + 1);
            append00(timestampBuffer, c.get(Calendar.DAY_OF_MONTH));
            timestampBuffer.put((byte) '-');
            append00(timestampBuffer, c.get(Calendar.HOUR_OF_DAY));
            append00(timestampBuffer, c.get(Calendar.MINUTE));
            append00(timestampBuffer, c.get(Calendar.SECOND));
            timestampBuffer.put((byte) '.');
            append000(timestampBuffer, c.get(Calendar.MILLISECOND));
            timestampBuffer.put((byte) ' ');
            timestampBuffer.flip();
            channel.write(timestampBuffer);

            timestampBuffer.clear();
        }

        ByteBuffer packetSizeBuffer = packetSizeBuffer_;
        if (packetSizeBuffer != null) {
            packetSizeBuffer.putInt(size).flip();
            channel.write(packetSizeBuffer);
            packetSizeBuffer.clear();
        }
    }

    /**
     * Appends the separator.
     *
     * @param channel the channel to be written into
     * @param size the size
     * @param now the time in milliseconds
     * @throws IOException
     */
    public void append(GatheringByteChannel channel, int size, long now) throws IOException {
        ByteBuffer buffer = separatorBuffer_;
        channel.write(buffer);
        buffer.clear();
    }

    static void append0000(ByteBuffer buffer, int year) {
        buffer.put((byte) (year / 1000 + '0'))
                .put((byte) ((year / 100 % 10) + '0'))
                .put((byte) ((year / 10 % 10) + '0'))
                .put((byte) ((year % 10) + '0'));
    }

    static void append00(ByteBuffer buffer, int n) {
        if (n >= 10) {
            buffer.put((byte) ((n / 10) + '0')).put((byte) ((n % 10) + '0'));
        } else {
            buffer.put((byte) '0').put((byte) (n + '0'));
        }
    }

    static void append000(ByteBuffer buffer, int n) {
        if (n >= 100) {
            buffer.put((byte) ((n / 100) + '0'))
                    .put((byte) ((n / 10 % 10) + '0'))
                    .put((byte) ((n % 10) + '0'));
        } else if (n >= 10) {
            buffer.put((byte) '0')
                    .put((byte) ((n / 10) + '0'))
                    .put((byte) ((n % 10) + '0'));
        } else {
            buffer.put((byte) '0')
                    .put((byte) '0')
                    .put((byte) (n + '0'));
        }
    }
}
