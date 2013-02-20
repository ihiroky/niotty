package net.ihiroky.niotty.buffer;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Created on 13/02/08, 16:14
 *
 * @author Hiroki Itoh
 */
public class ArrayBufferSink implements BufferSink {

    private byte[] buffer;
    private int offset;
    private int end;

    ArrayBufferSink(byte[] buffer, int offset, int length) {
        this.buffer = buffer;
        this.offset = offset;
        this.end = offset + length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        int remaining = end - offset;
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
                offset += readyToWrite;
                return false;
            }
            writeBuffer.clear();
        }
        offset = end;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remainingBytes() {
        return end - offset;
    }
}
