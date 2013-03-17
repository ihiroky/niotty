package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface StageContextExecutor {
    <I> void execute(StageContext<I, ?> context, I input);
    <I> void execute(StageContext<I, ?> context, TransportStateEvent event);
    StageContextExecutorPool pool();
    void close(StageContext<?, ?> context);
}
