package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * @author Hiroki Itoh
 */
public interface NioWriteQueue {
    boolean offer(BufferSink bufferSink);
    FlushStatus flushTo(WritableByteChannel channel) throws IOException;
    int size();
    boolean isEmpty();
    int lastFlushedBytes();

    enum FlushStatus {
        FLUSHED(0),
        FLUSHING(100),
        SKIP(-1),
        ;

        int waitTimeMillis_;

        private FlushStatus(int waitTimeMillis) {
            waitTimeMillis_ = waitTimeMillis;
        }

        public int waitTimeMillis() {
            return waitTimeMillis_;
        }
    }
}
