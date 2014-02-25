package net.ihiroky.niotty.nio.unix;

import net.ihiroky.niotty.util.Platform;

import java.nio.ByteBuffer;

/**
 * TODO test
 */
class BufferCache {

    private ByteBuffer[] buffers_;
    private int head_;
    private int count_;

    private static final ThreadLocal<BufferCache> instanceHolder_ = new ThreadLocal<BufferCache>() {
        @Override
        protected BufferCache initialValue() {
            return new BufferCache();
        }
    };

    static BufferCache getInstance() {
        return instanceHolder_.get();
    }

    static synchronized BufferCache newInstance() {
        BufferCache instance = instanceHolder_.get();
        if (instance == null) {
            instance = new BufferCache();
            instanceHolder_.set(instance);
        }
        return instance;
    }

    private BufferCache() {
        int size = 1;
        while (size < Native.IOV_MAX) {
            size <<= 1;
        }

        buffers_ = new ByteBuffer[size];
    }

    ByteBuffer search(int size) {
        int head = head_;
        if (count_ == 0) {
            return ByteBuffer.allocateDirect(size);
        }

        ByteBuffer[] buffers = buffers_;
        int mask = buffers.length - 1;
        int i = head;
        int tail = (head + count_) % mask;
        int minimum = i;
        ByteBuffer b = buffers[i];
        if (b.capacity() < size) {
            i = (head + 1) & mask;
            while (i != tail) {
                b = buffers[i];
                if (b.capacity() >= size) {
                    buffers[i] = buffers[head];
                    break;
                }
                if (b.capacity() < minimum) {
                    minimum = i;
                }
                i = (i + 1) & mask;
            }

            if (i == tail) {
                // Release the minimum buffer to avoid the cache growing.
                b = buffers[minimum];
                buffers[minimum] = buffers[head];
                buffers[head] = null;
                head_ = (head + 1) & mask;
                count_--;
                Platform.release(b);
                return ByteBuffer.allocateDirect(size);
            }
        }
        buffers[head] = null;
        head_ = (head + 1) & mask;
        count_--;
        b.rewind().limit(size);
        return b;
    }

    void offerFirst(ByteBuffer buffer) {
        if (count_ == Native.IOV_MAX) {
            Platform.release(buffer);
            return;
        }

        ByteBuffer[] buffers = buffers_;
        int mask = buffers.length - 1;
        int h = (head_ - 1) & mask;
        buffers[h] = buffer;
        head_ = h;
        count_++;
    }

    void offerLast(ByteBuffer buffer) {
        if (count_ == Native.IOV_MAX) {
            Platform.release(buffer);
            return;
        }

        ByteBuffer[] buffers = buffers_;
        int mask = buffers.length - 1;
        int t = (head_ + count_) & mask;
        buffers[t] = buffer;
        count_++;
    }

    int count() {
        return count_;
    }

    void clear() {
        ByteBuffer[] buffers = buffers_;
        for (int i = head_; buffers[i] != null;) {
            Platform.release(buffers[i]);
            i = (i + 1) & buffers.length;
            buffers[i] = null;
        }
        head_ = 0;
        count_ = 0;
    }

    void dispose() {
        clear();
        instanceHolder_.remove();
    }
}
