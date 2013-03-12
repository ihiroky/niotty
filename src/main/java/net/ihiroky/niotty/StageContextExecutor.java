package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface StageContextExecutor<I> {
    void execute(StageContext<I, ?> context, I input);
    void execute(StageContext<I, ?> context, TransportStateEvent event);
    void invalidate(StageContext<I, ?> context);
    void start();
    void stop();
}
