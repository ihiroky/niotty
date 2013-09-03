package net.ihiroky.niotty;

/**
 * A pipeline for the {@link LoadStage}.
 *
 * @param <L> the type of the TaskLoop which executes the stages by default
 */
public class DefaultLoadPipeline<L extends TaskLoop>
        extends AbstractPipeline<LoadStage<?, ?>, L> implements LoadPipeline {

    private static final String SUFFIX_LOAD = "[load]";
    private static final StageKey TAIL_STAGE = StageKeys.of("LOAD_TAIL_STAGE");

    public DefaultLoadPipeline(
            String name, AbstractTransport<L> transport, TaskLoopGroup<? extends TaskLoop> taskLoopGroup) {
        super(String.valueOf(name).concat(SUFFIX_LOAD), transport, taskLoopGroup);
    }

    @Override
    protected PipelineElement<Object, Object> createContext(
            StageKey key, LoadStage<?, ?> stage, PipelineElementExecutorPool pool) {
        @SuppressWarnings("unchecked")
        LoadStage<Object, Object> s = (LoadStage<Object, Object>) stage;
        return new LoadStageContext<>(this, key, s, pool);
    }

    @Override
    protected Tail<LoadStage<?, ?>> createTail(PipelineElementExecutorPool defaultPool) {
        return new LoadTail(this, TAIL_STAGE, AbstractPipeline.NULL_POOL);
    }

    private static class LoadTail extends Tail<LoadStage<?, ?>> {
        protected LoadTail(AbstractPipeline<?, ?> pipeline, StageKey key, PipelineElementExecutorPool pool) {
            super(pipeline, key, pool);
        }

        @Override
        void setStage(LoadStage<?, ?> stage) {
        }

        @Override
        protected Object stage() {
            return null;
        }

        @Override
        protected void fire(Object input) {
        }

        @Override
        protected void fire(Object input, TransportParameter parameter) {
        }

        @Override
        protected void fire(TransportStateEvent event) {
        }

        @Override
        public TransportParameter transportParameter() {
            return DefaultTransportParameter.NO_PARAMETER;
        }
    }
}
