package net.ihiroky.niotty.buffer;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * @author Hiroki Itoh
 */
public class ByteBufferBufferSink implements BufferSink {

    private ByteBuffer byteBuffer_;

    ByteBufferBufferSink(ByteBuffer byteBuffer) {
        this.byteBuffer_ = byteBuffer;
    }

    @Override
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        ByteBuffer localByteBuffer = byteBuffer_;
        int remaining = localByteBuffer.remaining();
        while (remaining > 0) {
            int limit = byteBuffer_.limit();
            int space = writeBuffer.remaining();
            int readyToWrite = (remaining <= space) ? remaining : space;
            localByteBuffer.limit(localByteBuffer.position() + readyToWrite);
            writeBuffer.put(localByteBuffer);
            writeBuffer.flip();
            int writeBytes = channel.write(writeBuffer);
            if (writeBytes == -1) {
                localByteBuffer.limit(limit);
                throw new EOFException();
            }
            // Some bytes remains in writeBuffer. Stop this round.
            if (writeBytes < readyToWrite) {
                localByteBuffer.limit(limit);
                return false;
            }
            // Write all bytes in writeBuffer.
            remaining -= writeBytes;
            writeBuffer.clear();
        }
        return true;
    }

    @Override
    public int remainingBytes() {
        return byteBuffer_.remaining();
    }
}
