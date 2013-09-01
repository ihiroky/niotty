package net.ihiroky.niotty;

import net.ihiroky.niotty.buffer.BufferSink;

import java.util.Iterator;

/**
 * Created on 13/01/10, 17:21
 *
 * @author Hiroki Itoh
 */
public class DefaultStorePipeline<L extends TaskLoop>
        extends AbstractPipeline<StoreStage<?, ?>, L> implements StorePipeline {

    private static final String SUFFIX_STORE = "[store]";
    private static final StageKey IO_STAGE_KEY = StageKeys.of("IO_STAGE");

    public DefaultStorePipeline(
            String name, AbstractTransport<L> transport, TaskLoopGroup<? extends TaskLoop> taskLoopGroup) {
        super(String.valueOf(name).concat(SUFFIX_STORE), transport, taskLoopGroup);
    }

    @Override
    protected PipelineElement<Object, Object> createContext(
            StageKey key, StoreStage<?, ?> stage, PipelineElementExecutorPool pool) {
        @SuppressWarnings("unchecked")
        StoreStage<Object, Object> s = (StoreStage<Object, Object>) stage;
        return new StoreStageContext<>(this, key, s, pool);
    }

    public void addIOStage(StoreStage<?, Void> stage) {
        super.add(IO_STAGE_KEY, stage);
    }

    @SuppressWarnings("unchecked")
    public StoreStage<BufferSink, Void> searchIOStage() {
        return (StoreStage<BufferSink, Void>) super.search(IO_STAGE_KEY);
    }

    public DefaultStorePipeline<L> createCopy() {
        DefaultStorePipeline<L> copy = new DefaultStorePipeline<>(name(), transport(), taskLoopGroup());
        for (Iterator<PipelineElement<Object, Object>> i = iterator(); i.hasNext();) {
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
