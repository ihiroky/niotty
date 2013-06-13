package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.TaskLoopGroup;

import java.nio.channels.SelectableChannel;

/**
 * Created on 13/01/10, 18:43
 *
 * @author Hiroki Itoh
 */
public abstract class AbstractSelectorPool<S extends AbstractSelector<S>> extends TaskLoopGroup<S> {

    public void register(final SelectableChannel channel, final int ops, final NioSocketTransport<S> transport) {
        S target = searchLowMemberCountLoop();
        if (target == null) {
            throw new AssertionError("I should not reach here. Task threads may be stopped.");
        }
        transport.addIOStage(target);
        transport.setEventLoop(target);
        target.offerTask(new TaskLoop.Task<S>() {
            @Override
            public int execute(S eventLoop) {
                eventLoop.register(channel, ops, transport);
                return TaskLoop.TIMEOUT_NO_LIMIT;
            }
        });
    }
}
