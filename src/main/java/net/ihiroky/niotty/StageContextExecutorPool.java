package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface StageContextExecutorPool {
    StageContextExecutor assign(StageContext<?, ?> context);
    void shutdown();
}
