package net.ihiroky.niotty;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Hiroki Itoh
 */
public class ThreadPoolStageContextExecutor implements StageContextExecutor<Object> {

    private ExecutorService threadPool_;

    public static ThreadPoolStageContextExecutor newSingleThreadStageContextExecutor(String name) {
        return new ThreadPoolStageContextExecutor(name, 1);
    }

    public ThreadPoolStageContextExecutor(String name, int numberOfThread) {
        threadPool_ = Executors.newFixedThreadPool(numberOfThread, new NameCountThreadFactory(name));
    }

    @Override
    public void execute(final StageContext<Object, ?> context, final Object input) {
        threadPool_.execute(new Runnable() {
            @Override
            public void run() {
                context.fire(input);
            }
        });
    }

    @Override
    public void execute(final StageContext<Object, ?> context, final TransportStateEvent event) {
        threadPool_.execute(new Runnable() {
            @Override
            public void run() {
                context.fire(event);
            }
        });
    }

    @Override
    public void invalidate(StageContext<Object, ?> context) {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        threadPool_.shutdownNow();
        threadPool_ = null;
    }
}
