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

    @Override
    protected Tail<StoreStage<?, ?>> createTail(PipelineElementExecutorPool defaultPool) {
        return new StoreTail(this, IO_STAGE_KEY, defaultPool);
    }

    private static class StoreTail extends Tail<StoreStage<?, ?>> {

        private StoreStage<Object, Object> stage_ = NULL;

        private static final StoreStage<Object, Object> NULL = new StoreStage<Object, Object>() {
            @Override
            public void store(StageContext<Object> context, Object input) {
                throw new IllegalStateException("No I/O stage is initialized. It may not be connected?");
            }

            @Override
            public void store(StageContext<Object> context, TransportStateEvent event) {
                throw new IllegalStateException("No I/O stage is initialized. It may not be connected?");
            }
        };

        protected StoreTail(AbstractPipeline<?, ?> pipeline, StageKey key, PipelineElementExecutorPool pool) {
            super(pipeline, key, pool);
        }

        @Override
        @SuppressWarnings("unchecked")
        void setStage(StoreStage<?, ?> stage) {
            stage_ = (StoreStage<Object, Object>) stage;
        }

        @Override
        protected Object stage() {
            return stage_;
        }

        @Override
        protected void fire(Object input) {
            stage_.store(this, input);
        }

        @Override
        protected void fire(Object input, TransportParameter parameter) {
            WrappedStageContext<Object> context = new WrappedStageContext<>(this, parameter);
            stage_.store(context, input);
        }

        @Override
        protected void fire(TransportStateEvent event) {
            stage_.store(this, event);
        }

        @Override
        public TransportParameter transportParameter() {
            return DefaultTransportParameter.NO_PARAMETER;
        }
    }
}
