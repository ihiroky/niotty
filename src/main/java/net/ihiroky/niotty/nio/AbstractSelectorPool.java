package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.TaskLoopGroup;

import java.nio.channels.SelectableChannel;

/**
 * Created on 13/01/10, 18:43
 *
 * @author Hiroki Itoh
 */
public abstract class AbstractSelectorPool<S extends AbstractSelector> extends TaskLoopGroup<S> {

    public void register(final SelectableChannel channel, final int ops, final NioSocketTransport<S> transport) {
        final S target = assign(transport);
        if (target == null) {
            throw new AssertionError("I should not reach here. Task threads may be stopped.");
        }
        transport.addIOStage(target);
        transport.setTaskLoop(target);
        target.offerTask(new TaskLoop.Task() {
            @Override
            public int execute() {
                target.register(channel, ops, transport);
                return TaskLoop.WAIT_NO_LIMIT;
            }
        });
    }
}
