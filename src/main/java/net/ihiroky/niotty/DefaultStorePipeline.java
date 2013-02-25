package net.ihiroky.niotty;

/**
 * Created on 13/01/10, 17:21
 *
 * @author Hiroki Itoh
 */
public class DefaultStorePipeline extends AbstractPipeline<StoreStage<Object, Object>> implements StorePipeline {

    private static final String SUFFIX_STORE = "[store]";

    public static DefaultStorePipeline createPipeline(String name) {
        return new DefaultStorePipeline(String.valueOf(name).concat(SUFFIX_STORE));
    }

    protected DefaultStorePipeline(String name) {
        super(name);
    }

    @Override
    protected StageContext<Object, Object> createContext(StoreStage<Object, Object> stage) {
        return new StoreStageContext<>(this, stage);
    }

    @Override
    public StorePipeline add(StoreStage<?, ?> stage) {
        @SuppressWarnings("unchecked")
        StoreStage<Object, Object> s = (StoreStage<Object, Object>) stage;
        super.addStage(s);
        return this;
    }
}
