package net.ihiroky.niotty.nio;

/**
 * @author Hiroki Itoh
 */
public class UdpIOSelectorPool extends AbstractSelectorPool<IOSelector> {

    private int readBufferSize_;
    private int writeBufferSize_;
    private boolean direct_;

    private static final int DEFAULT_BUFFER_SIZE = Short.MAX_VALUE << 1;

    public UdpIOSelectorPool() {
        readBufferSize_ = DEFAULT_BUFFER_SIZE;
        writeBufferSize_ = DEFAULT_BUFFER_SIZE;
        direct_ = false;
    }

    public void setReadBufferSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive.");
        }
        readBufferSize_ = size;
    }

    public void setWriteBufferSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive.");
        }
        writeBufferSize_ = size;
    }

    public void setDirect(boolean direct) {
        direct_ = direct;
    }

    @Override
    protected IOSelector newEventLoop() {
        return new IOSelector(new UdpIOStoreStage(writeBufferSize_, direct_), readBufferSize_, direct_);
    }
}
