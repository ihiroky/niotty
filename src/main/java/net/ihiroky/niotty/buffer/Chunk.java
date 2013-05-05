package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;

/**
*  TODO introduce reference count in chunk and implements duplicate() for CodecBuffer#slice()?
* @author Hiroki Itoh
*/
abstract class Chunk<E> {

    final E buffer_;
    final BufferAllocator<E> allocator_;
    boolean released_;

    private static final BufferAllocator<?> NULL_ALLOCATOR = new BufferAllocator<Object>() {
        @Override
        public Chunk<Object> allocate(int bytes) {
            throw new UnsupportedOperationException("I am null allocator.");
        }
        @Override
        public void release(Chunk<Object> chunk) {
        }
        @Override
        public void free() {
        }
    };

    @SuppressWarnings("unchecked")
    Chunk(E buffer) {
        buffer_ = buffer;
        allocator_ = (BufferAllocator<E>) NULL_ALLOCATOR;
    }

    @SuppressWarnings("unchecked")
    Chunk(E buffer, BufferAllocator<E> allocator) {
        buffer_ = buffer;
        allocator_ = (allocator != null) ? allocator : (BufferAllocator<E>) NULL_ALLOCATOR;
    }

    E buffer() {
        return buffer_;
    }

    void release() {
        synchronized (this) {
            if (!released_) {
                allocator_.release(this);
                released_ = true;
            }
        }
    }

    abstract E duplicate();
    abstract int size();

    Chunk<E> newChunk(int bytes) {
        // TODO allocate and release
        return allocator_.allocate(bytes);
    }

    static class ByteArrayChunk extends Chunk<byte[]> {

        ByteArrayChunk(byte[] buffer) {
            super(buffer);
        }

        ByteArrayChunk(byte[] buffer, BufferAllocator<byte[]> allocator) {
            super(buffer, allocator);
        }

        @Override
        byte[] duplicate() {
            return buffer_;
        }

        @Override
        int size() {
            return buffer_.length;
        }
    }

    static class ByteBufferChunk extends Chunk<ByteBuffer> {

        ByteBufferChunk(ByteBuffer buffer) {
            super(buffer);
        }

        ByteBufferChunk(ByteBuffer buffer, BufferAllocator<ByteBuffer> allocator) {
            super(buffer, allocator);
        }

        @Override
        ByteBuffer duplicate() {
            return buffer_.duplicate();
        }

        @Override
        int size() {
            return buffer_.capacity();
        }
    }
}
