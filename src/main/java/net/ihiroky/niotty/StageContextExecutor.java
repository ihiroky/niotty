package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface StageContextExecutor {
    <I> void execute(StageContext<I, ?> context, I input);
    void execute(StageContext<?, ?> context, TransportStateEvent event);
    StageContextExecutorPool pool();
    void close(StageContext<?, ?> context);
}
