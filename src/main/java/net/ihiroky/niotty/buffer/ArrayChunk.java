package net.ihiroky.niotty.buffer;

/**
 * A implementation of {@link net.ihiroky.niotty.buffer.Chunk} that holds an byte array.
 * @author Hiroki Itoh
 */
public class ArrayChunk extends AbstractChunk<byte[]> {

    /**
     * Constructs this instance.
     * @param buffer the byte array to be managed by this instance
     * @param manager the manager to manage this instance
     */
    public ArrayChunk(byte[] buffer, ChunkManager<byte[]> manager) {
        super(buffer, manager);
    }

    @Override
    public byte[] retain() {
        incrementReferenceCount();
        return buffer_;
    }

    @Override
    public int size() {
        return buffer_.length;
    }
}
