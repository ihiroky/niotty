package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.util.Arguments;

/**
 * @author Hiroki Itoh
 */
public class UdpIOSelectorPool extends AbstractSelectorPool<UdpIOSelector> {

    private int readBufferSize_;
    private int writeBufferSize_;
    private boolean direct_;
    private boolean duplicateBuffer_;

    private static final int DEFAULT_BUFFER_SIZE = Short.MAX_VALUE;

    public UdpIOSelectorPool() {
        readBufferSize_ = DEFAULT_BUFFER_SIZE;
        writeBufferSize_ = DEFAULT_BUFFER_SIZE;
        direct_ = false;
        duplicateBuffer_ = true;
    }

    public int readBufferSize() {
        return readBufferSize_;
    }

    public void setReadBufferSize(int readBufferSize) {
        readBufferSize_ = Arguments.requirePositive(readBufferSize, "readBufferSize");
    }

    public int writeBufferSize() {
        return writeBufferSize_;
    }

    public void setWriteBufferSize(int writeBufferSize) {
        writeBufferSize_ = Arguments.requirePositive(writeBufferSize, "writeBufferSize");
    }

    public boolean direct() {
        return direct_;
    }

    public void setDirect(boolean direct) {
        direct_ = direct;
    }

    public boolean duplicateBuffer() {
        return duplicateBuffer_;
    }

    public void setDuplicateBuffer(boolean duplicateBuffer) {
        duplicateBuffer_ = duplicateBuffer;
    }

    @Override
    protected UdpIOSelector newTaskLoop() {
        return new UdpIOSelector(readBufferSize_, writeBufferSize_, direct_, duplicateBuffer_);
    }
}
