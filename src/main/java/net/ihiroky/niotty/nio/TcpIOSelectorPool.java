package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.util.Arguments;

/**
 * @author Hiroki Itoh
 */
public class TcpIOSelectorPool extends AbstractSelectorPool<TcpIOSelector> {

    private int readBufferSize_;
    private boolean direct_;
    private boolean duplicateBuffer_;

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    public TcpIOSelectorPool() {
        readBufferSize_ = DEFAULT_BUFFER_SIZE;
        direct_ = false;
        duplicateBuffer_ = true;
    }

    public int readBufferSize() {
        return readBufferSize_;
    }

    public void setReadBufferSize(int readBufferSize) {
        readBufferSize_ = Arguments.requirePositive(readBufferSize, "readBufferSize");
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
    protected TcpIOSelector newTaskLoop() {
        return new TcpIOSelector(readBufferSize_, direct_, duplicateBuffer_);
    }
}
