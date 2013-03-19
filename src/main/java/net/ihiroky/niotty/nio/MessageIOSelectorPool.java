package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventLoop;

import java.nio.channels.SelectableChannel;

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

    @Override
    public void register(final SelectableChannel channel, final int ops,
                         final NioSocketTransport<MessageIOSelector> transport) {
        MessageIOSelector target = searchLowMemberCountLoop();
        if (target == null) {
            throw new AssertionError("should not reach here.");
        }
        transport.addIOStage(target.ioStoreStage());
        transport.setEventLoop(target);
        target.offerTask(new EventLoop.Task<MessageIOSelector>() {
                @Override
                public boolean execute(MessageIOSelector eventLoop) {
                    eventLoop.register(channel, ops, transport);
                    return true;
                }
        });
    }
}
