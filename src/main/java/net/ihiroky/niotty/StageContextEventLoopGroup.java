package net.ihiroky.niotty;

import java.util.List;

/**
 * @author Hiroki Itoh
 */
public class StageContextEventLoopGroup extends EventLoopGroup<StageContextEventLoop> {

    private final Object allocateMutex_ = new Object();

    @Override
    protected StageContextEventLoop newEventLoop() {
        return new StageContextEventLoop(allocateMutex_);
    }

    StageContextExecutor<Object> allocateStageContextExecutorFor(StageContext<Object, ?> context) {
        synchronized (allocateMutex_) {
            List<StageContextEventLoop> loops = sortedLoopsView();
            StageContextEventLoop target = loops.get(0);
            for (StageContextEventLoop loop : loops) {
                if (loop.contains(context)) {
                    target = loop;
                    break;
                }
            }
            target.add(context);
            return target;
        }
    }
}
