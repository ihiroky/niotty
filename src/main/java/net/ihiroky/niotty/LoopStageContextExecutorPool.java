package net.ihiroky.niotty;

import java.util.List;

/**
 * @author Hiroki Itoh
 */
public class LoopStageContextExecutorPool extends EventLoopGroup<LoopStageContextExecutor> implements StageContextExecutorPool {

    private final Object allocationLock_ = new Object();

    @Override
    protected LoopStageContextExecutor newEventLoop() {
        return new LoopStageContextExecutor(this);
    }

    Object allocationLock() {
        return allocationLock_;
    }

    @Override
    public StageContextExecutor assign(StageContext<?, ?> context) {
        synchronized (allocationLock_) {
            List<LoopStageContextExecutor> loops = sortedLoopsView();
            LoopStageContextExecutor target = loops.get(0);
            for (LoopStageContextExecutor loop : loops) {
                if (loop.contains(context)) {
                    target = loop;
                    break;
                }
            }
            target.accept(context);
            return target;
        }
    }

    @Override
    public void shutdown() {
        super.close();
    }
}
