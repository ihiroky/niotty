package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public final class DefaultStageContextExecutorPool implements StageContextExecutorPool {

    private static final DefaultStageContextExecutorPool INSTANCE = new DefaultStageContextExecutorPool();
    private static final DefaultStageContextExecutor EXECUTOR = new DefaultStageContextExecutor();

    private DefaultStageContextExecutorPool() {
    }

    public static DefaultStageContextExecutorPool instance() {
        return INSTANCE;
    }

    @Override
    public StageContextExecutor assign(StageContext<?, ?> context) {
        return EXECUTOR;
    }

    @Override
    public void shutdown() {
    }
}
