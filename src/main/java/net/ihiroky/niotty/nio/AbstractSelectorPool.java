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

    public void register(final NioSocketTransport<S> transport, final SelectableChannel channel, final int ops) {
        int min = Integer.MAX_VALUE;
        AbstractSelector<S> target = null;
        for (AbstractSelector<S> loop : eventLoops()) {
            int registered = loop.getRegisteredCount();
            if (registered < min) {
                min = registered;
                target = loop;
            }
        }
        if (target != null) {
            transport.setSelector(target);
            target.offerTask(new EventLoop.Task<S>() {
                @Override
                public boolean execute(S eventLoop) {
                    eventLoop.register(transport, channel, ops);
                    return true;
                }
            });
        }

    }
}
