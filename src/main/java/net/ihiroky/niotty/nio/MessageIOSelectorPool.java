package net.ihiroky.niotty.nio;

/**
 * Created on 13/01/15, 17:07
 *
 * @author Hiroki Itoh
 */
public class MessageIOSelectorPool extends AbstractSelectorPool<MessageIOSelector> {

    private int readBufferSize_;
    private int writeBufferSize_;
    private boolean direct_;

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    public MessageIOSelectorPool() {
        readBufferSize_ = DEFAULT_BUFFER_SIZE;
        writeBufferSize_ = DEFAULT_BUFFER_SIZE;
        direct_ = false;
    }

    public void setReadBufferSize(int size) {
        if (readBufferSize_ <= 0) {
            throw new IllegalArgumentException("readBufferSize must be positive.");
        }
        readBufferSize_ = size;
    }

    public void setDirect(boolean on) {
        this.direct_ = on;
    }

    @Override
    protected MessageIOSelector newEventLoop() {
        return new MessageIOSelector(readBufferSize_, writeBufferSize_, direct_);
    }
}
