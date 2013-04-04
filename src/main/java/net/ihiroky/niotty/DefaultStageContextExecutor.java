package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public class DefaultStageContextExecutor implements StageContextExecutor {
    @Override
    public <I> void execute(StageContext<I, ?> context, I input) {
        context.fire(input);
    }

    @Override
    public <I> void execute(StageContext<I, ?> context, TransportStateEvent event) {
        context.fire(event);
    }

    @Override
    public StageContextExecutorPool pool() {
        return DefaultStageContextExecutorPool.instance();
    }

    @Override
    public void close(StageContext<?, ?> context) {
    }
}
