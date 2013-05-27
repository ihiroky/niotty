package net.ihiroky.niotty;

import java.util.Iterator;

/**
 * Created on 13/01/10, 17:21
 *
 * @author Hiroki Itoh
 */
public class DefaultLoadPipeline extends AbstractPipeline<LoadStage<?, ?>> implements LoadPipeline {

    private static final String SUFFIX_LOAD = "[load]";

    public DefaultLoadPipeline(String name, Transport transport) {
        super(String.valueOf(name).concat(SUFFIX_LOAD), transport);
    }

    @Override
    protected PipelineElement<Object, Object> createContext(
            StageKey key, LoadStage<?, ?> stage, PipelineElementExecutorPool pool) {
        @SuppressWarnings("unchecked")
        LoadStage<Object, Object> s = (LoadStage<Object, Object>) stage;
        return new LoadStageContext<>(this, key, s, pool);
    }

    public DefaultLoadPipeline createCopy() {
        DefaultLoadPipeline copy = new DefaultLoadPipeline(name(), transport());
        for (Iterator<PipelineElement<Object, Object>> i = iterator(); i.hasNext();) {
            @SuppressWarnings("unchecked")
            LoadStageContext<Object, Object> context = (LoadStageContext<Object, Object>) i.next();
            copy.add(context.key(), context.stage(), context.executor().pool());
        }
        return copy;
    }
}
