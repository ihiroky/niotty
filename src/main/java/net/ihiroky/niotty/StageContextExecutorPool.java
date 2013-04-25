package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface StageContextExecutorPool extends AutoCloseable {
    StageContextExecutor assign(StageContext<?, ?> context);
    void close();
}
