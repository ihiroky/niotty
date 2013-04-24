package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface StageContextExecutorPool extends Cloneable {
    StageContextExecutor assign(StageContext<?, ?> context);
    void close();
}
