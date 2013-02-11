package net.ihiroky.niotty.buffer;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Created on 13/02/01, 17:38
 *
 * @author Hiroki Itoh
 */
class SegmentedArrayBufferSink implements BufferSink {

    private byte[][] buffers;
    private int bufferIndex;
    private int countInBuffer;
    private final int lastBufferIndex;
    private final int lastCountInBuffer;

    static final SegmentedArrayBufferSink EMPTY_ENCODING_BUFFER = new SegmentedArrayBufferSink(new byte[1][0]);

    SegmentedArrayBufferSink(byte[] buffer) {
        this(new byte[][] {buffer});
    }

    SegmentedArrayBufferSink(byte[][] buffers) {
        this(buffers, buffers.length - 1, buffers[buffers.length - 1].length);
    }

    SegmentedArrayBufferSink(byte[][] buffers, int lastBufferIndex, int lastCountInBuffer) {
        this.buffers = buffers;
        this.lastBufferIndex = lastBufferIndex;
        this.lastCountInBuffer = lastCountInBuffer;
    }

    @Override
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        int cib = countInBuffer;
        int biLast = lastBufferIndex;
        for (int bi = bufferIndex; bi < biLast; bi++) {
            byte[] buffer = buffers[bi];
            cib = transferBuffer(channel, writeBuffer, buffer, cib, buffer.length);
            if (cib != 0) {
                countInBuffer = cib;
                return false;
            }
        }
        transferBuffer(channel, writeBuffer, buffers[biLast], cib, lastCountInBuffer);
        bufferIndex = lastBufferIndex;
        countInBuffer = 0;
        return true;
    }

    @Override
    public int remainingBytes() {
        if (bufferIndex == buffers.length) {
            return 0;
        }
        if (bufferIndex == lastBufferIndex) {
            return lastCountInBuffer - countInBuffer;
        }

        long sum = 0;
        // current buffer remainingBytes
        sum = buffers[bufferIndex].length - countInBuffer;
        // middle buffers remainingBytes
        for (int i = bufferIndex + 1; i < lastBufferIndex; i++) {
            sum += buffers[i].length;
        }
        // last buffer remainingBytes
        sum += lastCountInBuffer;
        return (sum <= Integer.MAX_VALUE) ? (int) sum : Integer.MAX_VALUE;
    }

    private static int transferBuffer(WritableByteChannel channel, ByteBuffer writeBuffer,
                                      byte[] buffer, int offset, int length) throws IOException {
        int remaining = length - offset;
        while (remaining > 0) {
            int space = writeBuffer.remaining();
            int readyToWrite = (remaining <= space) ? remaining : space;
            writeBuffer.put(buffer, offset, readyToWrite);
            writeBuffer.flip();
            int writeBytes = channel.write(writeBuffer);
            // Write operation is failed.
            if (writeBytes == -1) {
                throw new EOFException();
            }
            remaining -= writeBytes;
            // Some bytes remains in writeBuffer. Stop this round
            if (writeBytes < readyToWrite) {
                return length - remaining;
            }
            // Write all bytes in writeBuffer. Prepare to left bytes to write.
            writeBuffer.clear();
        }
        return 0;
    }

    @Override
    public String toString() {
        return "bufferIndex:" + bufferIndex + ", countInBuffer:" + countInBuffer
                + ", lastBufferIndex:" + lastBufferIndex + ", lastCountInBuffer:" + lastBufferIndex;
    }
}
