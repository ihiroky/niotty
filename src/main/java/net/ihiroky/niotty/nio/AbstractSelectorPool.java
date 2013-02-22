package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventLoop;
import net.ihiroky.niotty.EventLoopGroup;

import java.nio.channels.SelectableChannel;

/**
 * Created on 13/01/10, 18:43
 *
 * @author Hiroki Itoh
 */
public abstract class AbstractSelectorPool<S extends AbstractSelector<S>> extends EventLoopGroup<S> {

    protected final S searchLoop() {
        int min = Integer.MAX_VALUE;
        S target = null;
        for (S loop : eventLoops()) {
            int registered = loop.getRegisteredCount();
            if (registered < min) {
                min = registered;
                target = loop;
            }
        }
        return target;
    }

    public void register(final SelectableChannel channel, final int ops,
                         final TransportFutureAttachment<S> attachment) {
        S target = searchLoop();
        if (target != null) {
            attachment.getTransport().setEventLoop(target);
            target.offerTask(new EventLoop.Task<S>() {
                @Override
                public boolean execute(S eventLoop) {
                    eventLoop.register(channel, ops, attachment);
                    return true;
                }
            });
        }

    }
}
