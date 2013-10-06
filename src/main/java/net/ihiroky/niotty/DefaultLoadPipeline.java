package net.ihiroky.niotty;

/**
 * A pipeline for the {@link LoadStage}.
 *
 * @param <L> the type of the TaskLoop which executes the stages by default
 */
public class DefaultLoadPipeline<L extends TaskLoop>
        extends AbstractPipeline<LoadStage<?, ?>, L> implements LoadPipeline {

    private static final String SUFFIX_LOAD = "[load]";
    private static final StageKey TAIL_STAGE_KEY = StageKeys.of("LOAD_TAIL_STAGE");
    private static final LoadStage<Object, Object> TAIL_STAGE = new TailStage();

    public DefaultLoadPipeline(
            String name, AbstractTransport<L> transport, TaskLoopGroup<L> taskLoopGroup) {
        super(String.valueOf(name).concat(SUFFIX_LOAD), transport, taskLoopGroup, TAIL_STAGE_KEY, TAIL_STAGE);
    }

    @Override
    protected final PipelineElement<Object, Object> createContext(
            StageKey key, LoadStage<?, ?> stage, TaskLoopGroup<? extends TaskLoop> pool) {
        @SuppressWarnings("unchecked")
        LoadStage<Object, Object> s = (LoadStage<Object, Object>) stage;
        return new LoadStageContext<Object, Object>(this, key, s, pool);
    }

    private static class TailStage implements LoadStage<Object, Object> {
        @Override
        public void load(StageContext<Object> context, Object input) {
        }

        @Override
        public void load(StageContext<Object> context, TransportStateEvent event) {
        }
    }
}
