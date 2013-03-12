package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public class DefaultStageContextExecutor implements StageContextExecutor<Object> {
    @Override
    public void execute(StageContext<Object, ?> context, Object input) {
        context.fire(input);
    }

    @Override
    public void execute(StageContext<Object, ?> context, TransportStateEvent event) {
        context.fire(event);
    }

    @Override
    public void invalidate(StageContext<Object, ?> context) {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
