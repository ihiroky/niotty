package net.ihiroky.niotty;

/**
 * Created on 13/01/10, 17:21
 *
 * @author Hiroki Itoh
 */
public class DefaultStorePipeline extends AbstractPipeline<StoreStage<Object, Object>> implements StorePipeline {

    private static final String SUFFIX_STORE = "[store]";

    public static DefaultStorePipeline createPipeline(String name, Transport transport) {
        return new DefaultStorePipeline(String.valueOf(name).concat(SUFFIX_STORE), transport);
    }

    protected DefaultStorePipeline(String name, Transport transport) {
        super(name, transport);
    }

    @Override
    protected StageContext<Object, Object> createContext(
            StoreStage<Object, Object> stage, StageContextExecutor<Object> executor) {
        return new StoreStageContext<>(this, stage, executor);
    }

    @Override
    public StorePipeline add(StoreStage<?, ?> stage) {
        return add(stage, null);
    }

    @Override
    public StorePipeline add(StoreStage<?, ?> stage, StageContextExecutor<?> executor) {
        @SuppressWarnings("unchecked")
        StoreStage<Object, Object> s = (StoreStage<Object, Object>) stage;
        @SuppressWarnings("unchecked")
        StageContextExecutor<Object> e = (StageContextExecutor<Object>) executor;
        super.addStage(s, e);
        return this;
    }
}
