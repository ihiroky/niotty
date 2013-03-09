package net.ihiroky.niotty;

/**
 * Created on 13/01/10, 17:21
 *
 * @author Hiroki Itoh
 */
public class DefaultLoadPipeline extends AbstractPipeline<LoadStage<Object, Object>> implements LoadPipeline {

    private static final String SUFFIX_LOAD = "[load]";

    public static DefaultLoadPipeline createPipeline(String name, Transport transport) {
        return new DefaultLoadPipeline(String.valueOf(name).concat(SUFFIX_LOAD), transport);
    }

    protected DefaultLoadPipeline(String name, Transport transport) {
        super(name, transport);
    }

    @Override
    protected StageContext<Object, Object> createContext(LoadStage<Object, Object> stage) {
        return new LoadStageContext<>(this, stage);
    }

    @Override
    public LoadPipeline add(LoadStage<?, ?> stage) {
        @SuppressWarnings("unchecked")
        LoadStage<Object, Object> s = (LoadStage<Object, Object>) stage;
        super.addStage(s);
        return this;
    }
}
