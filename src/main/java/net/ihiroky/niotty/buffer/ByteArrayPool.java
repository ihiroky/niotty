package net.ihiroky.niotty.buffer;

/**
 * @author Hiroki Itoh
 */
public class ByteArrayPool extends BufferPool<byte[]> {

    ByteArrayPool(int wholeBytes, int maxChunkBytes, boolean aggressive) {
        super(wholeBytes, maxChunkBytes, aggressive);
    }

    @Override
    Chunk<byte[]> allocate(int bytes, boolean aggressive) {
        return (bytes <= maxChunkBytes() || aggressive)
                ? new Chunk.ByteArrayChunk(new byte[bytes], this)
                : new Chunk.ByteArrayChunk(new byte[bytes]);
    }

    @Override
    void dispose() {
    }
}
