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

    public void register(final SelectableChannel channel, final int ops,
                         final TransportFutureAttachment<S> attachment) {
        S target = searchLowMemberCountLoop();
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
