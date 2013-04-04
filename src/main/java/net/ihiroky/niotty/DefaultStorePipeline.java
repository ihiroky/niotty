package net.ihiroky.niotty;

import net.ihiroky.niotty.buffer.BufferSink;

import java.util.Iterator;

/**
 * Created on 13/01/10, 17:21
 *
 * @author Hiroki Itoh
 */
public class DefaultStorePipeline extends AbstractPipeline<StoreStage<?, ?>> implements StorePipeline {

    private static final String SUFFIX_STORE = "[store]";
    private static final StageKey IO_STAGE_KEY = StageKeys.of("IO_STAGE");

    public DefaultStorePipeline(String name, Transport transport) {
        super(String.valueOf(name).concat(SUFFIX_STORE), transport);
    }

    @Override
    protected StageContext<Object, Object> createContext(
            StageKey key, StoreStage<?, ?> stage, StageContextExecutorPool pool) {
        @SuppressWarnings("unchecked")
        StoreStage<Object, Object> s = (StoreStage<Object, Object>) stage;
        return new StoreStageContext<>(this, key, s, pool);
    }

    public void addIOStage(StoreStage<BufferSink, Void> stage) {
        super.add(IO_STAGE_KEY, stage);
    }

    @SuppressWarnings("unchecked")
    public StoreStage<BufferSink, Void> searchIOStage() {
        return (StoreStage<BufferSink, Void>) super.search(IO_STAGE_KEY);
    }

    public DefaultStorePipeline createCopy() {
        DefaultStorePipeline copy = new DefaultStorePipeline(name(), transport());
        for (Iterator<StageContext<Object, Object>> i = iterator(); i.hasNext();) {
            @SuppressWarnings("unchecked")
            StoreStageContext<Object, Object> context = (StoreStageContext<Object, Object>) i.next();
            StageKey key = context.key();
            if (key != IO_STAGE_KEY) {
                copy.add(key, context.stage(), context.executor().pool());
            }
        }
        return copy;
    }
}
