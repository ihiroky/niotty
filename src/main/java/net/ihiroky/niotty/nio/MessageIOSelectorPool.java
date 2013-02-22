package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventLoop;

import java.nio.channels.SelectableChannel;

/**
 * Created on 13/01/15, 17:07
 *
 * @author Hiroki Itoh
 */
public class MessageIOSelectorPool extends AbstractSelectorPool<MessageIOSelector> {

    private int readBufferSize;
    private int writeBufferSize; // write buffer size, different from buffer queue.
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

    public void register(final SelectableChannel channel, final int ops,
                         final NioChildChannelTransport transport) {
        MessageIOSelector target = searchLoop();
        if (target != null) {
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
}
