package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TaskLoopGroup;

import java.util.concurrent.ThreadFactory;

/**
 * Created on 13/01/15, 17:10
 *
 * @author Hiroki Itoh
 */
public class AcceptSelectorPool extends TaskLoopGroup<AcceptSelector> {

    AcceptSelectorPool(ThreadFactory threadFactory) {
        super(threadFactory, 1);
    }

    @Override
    protected AcceptSelector newTaskLoop() {
        return new AcceptSelector();
    }
}
