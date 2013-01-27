package net.ihiroky.niotty.nio;

/**
 * Created on 13/01/15, 17:07
 *
 * @author Hiroki Itoh
 */
public class MessageIOSelectorPool extends AbstractSelectorPool<MessageIOSelector> {

    private int readBufferSize;
    private boolean direct;

    public MessageIOSelectorPool() {
        readBufferSize = 8192;
        direct = false;
    }

    public void setReadBufferSize(int size) {
        if (readBufferSize <= 0) {
            throw new IllegalArgumentException("readBufferSize must be positive.");
        }
        readBufferSize = size;
    }

    public void setDirect(boolean on) {
        this.direct = on;
    }

    @Override
    protected MessageIOSelector newEventLoop() {
        return new MessageIOSelector(readBufferSize, direct);
    }
}
