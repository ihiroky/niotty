package net.ihiroky.niotty;

/**
 * A pipeline for the {@link StoreStage}.
 *
 * @param <L> the type of the TaskLoop which executes the stages by default
 */
public class DefaultStorePipeline<L extends TaskLoop>
        extends AbstractPipeline<StoreStage<?, ?>, L> implements StorePipeline {

    private static final String SUFFIX_STORE = "[store]";

    public DefaultStorePipeline(
            String name, AbstractTransport<L> transport, TaskLoopGroup<L> taskLoopGroup, StoreStage<?, ?> tailStage) {
        super(String.valueOf(name).concat(SUFFIX_STORE), transport, taskLoopGroup, IO_STAGE_KEY, tailStage);
    }

    @Override
    protected final PipelineElement<Object, Object> createContext(
            StageKey key, StoreStage<?, ?> stage, TaskLoopGroup<? extends TaskLoop> pool) {
        @SuppressWarnings("unchecked")
        StoreStage<Object, Object> s = (StoreStage<Object, Object>) stage;
        return new StoreStageContext<Object, Object>(this, key, s, pool);
    }
}
