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
class ArrayBufferSink implements BufferSink {

    private byte[][] buffers;
    private int bufferIndex;
    private int countInBuffer;
    private final int lastBufferIndex;
    private final int lastCountInBuffer;

    static final ArrayBufferSink EMPTY_ENCODING_BUFFER = new ArrayBufferSink(new byte[1][0]);

    ArrayBufferSink(byte[] buffer) {
        this(new byte[][] {buffer});
    }

    ArrayBufferSink(byte[][] buffers) {
        this(buffers, buffers.length - 1, buffers[buffers.length - 1].length);
    }

    ArrayBufferSink(byte[][] buffers, int lastBufferIndex, int lastCountInBuffer) {
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
            // Some bytes remains in writeBuffer. This round stops.
            if (writeBytes != readyToWrite) {
                return offset + writeBytes;
            }
            // Write all bytes in writeBuffer. Prepare to left bytes to write in buffer or next bank.
            remaining -= writeBytes;
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
