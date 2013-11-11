package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Arguments;

import java.util.concurrent.TimeUnit;

/**
 */
public class PipelineElement {

    private volatile PipelineElement next_;
    private volatile PipelineElement prev_;
    final Pipeline pipeline_;
    final StageKey key_;
    final Stage stage_;
    final TaskLoop taskLoop_;
    final StoreContext storeContext_;
    final LoadContext loadContext_;
    final StateContext stateContext_;

    private static final Pipeline NULL_PIPELINE = new NullPipeline();
    private static final StageKey NULL_STAGE_KEY = StageKeys.of("NullStage");
    private static final Stage NULL_STAGE = new NullStage();
    private static final TaskLoopGroup<TaskLoop> NULL_TASK_LOOP_GROUP = new NullPipelineElementExecutorPool();
    private static final PipelineElement TERMINAL = newNullObject();

    protected PipelineElement(Pipeline pipeline, StageKey key, Stage stage,
            TaskLoopGroup<? extends TaskLoop> pool) {
        pipeline_ = Arguments.requireNonNull(pipeline, "pipeline");
        key_ = Arguments.requireNonNull(key, "key");
        stage_ = Arguments.requireNonNull(stage, "stage");
        taskLoop_ = Arguments.requireNonNull(pool, "pool").assign(pipeline.transport());
        storeContext_ = new StoreContext(this);
        loadContext_ = new LoadContext(this);
        stateContext_ = new StateContext(this);
        next_ = TERMINAL;
        prev_ = TERMINAL;
    }

    static PipelineElement newNullObject() {
        return new PipelineElement(NULL_PIPELINE, NULL_STAGE_KEY, NULL_STAGE, NULL_TASK_LOOP_GROUP);
    }

    static Stage newNullStage() {
        return new NullStage();
    }

    public StageKey key() {
        return key_;
    }

    public void execute(Task task) {
        taskLoop_.execute(task);
    }

    public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
        return taskLoop_.schedule(task, timeout, timeUnit);
    }

    Stage stage() {
        return stage_;
    }

    boolean isValid() {
        return pipeline_ != NULL_PIPELINE;
    }

    void clearLink() {
        next_ = TERMINAL;
        prev_ = TERMINAL;
    }

    protected PipelineElement next() {
        return next_;
    }

    protected void setNext(PipelineElement next) {
        next_ = Arguments.requireNonNull(next, "next");
    }

    protected PipelineElement prev() {
        return prev_;
    }

    protected void setPrev(PipelineElement prev) {
        prev_ = Arguments.requireNonNull(prev, "prev");
    }

    void close() {
        taskLoop_.reject(pipeline_.transport());
    }

    void callStore(final Object message) {
        if (taskLoop_.isInLoopThread()) {
            stage_.stored(storeContext_, message);
        } else {
            taskLoop_.offer(new Task() {
                @Override
                public long execute(TimeUnit timeUnit) throws Exception {
                    stage_.stored(storeContext_, message);
                    return DONE;
                }
            });
        }
    }

    void callStore(final Object message, final Object parameter) {
        if (taskLoop_.isInLoopThread()) {
            StageContext context = new ParameterStoreStageContext(this, parameter);
            stage_.stored(context, message);
        } else {
            taskLoop_.offer(new Task() {
                @Override
                public long execute(TimeUnit timeUnit) throws Exception {
                    StageContext context = new ParameterStoreStageContext(PipelineElement.this, parameter);
                    stage_.stored(context, message);
                    return DONE;
                }
            });
        }
    }

    // expand to context class
    void callLoad(final Object message) {
        if (taskLoop_.isInLoopThread()) {
            stage_.loaded(loadContext_, message);
        } else {
            taskLoop_.offer(new Task() {
                @Override
                public long execute(TimeUnit timeUnit) throws Exception {
                    stage_.loaded(loadContext_, message);
                    return DONE;
                }
            });
        }
    }

    void callLoad(final Object message, final Object parameter) {
        if (taskLoop_.isInLoopThread()) {
            StageContext context = new ParameterLoadStageContext(this, parameter);
            stage_.loaded(context, message);
        } else {
            taskLoop_.offer(new Task() {
                @Override
                public long execute(TimeUnit timeUnit) throws Exception {
                    StageContext context = new ParameterLoadStageContext(PipelineElement.this, parameter);
                    stage_.loaded(context, message);
                    return DONE;
                }
            });
        }
    }

    void callActivate() {
        if (taskLoop_.isInLoopThread()) {
            stage_.activated(stateContext_);
            return;
        }
        taskLoop_.offer(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                stage_.activated(stateContext_);
                return DONE;
            }
        });
    }

    void callDeactivate(final DeactivateState state) {
        if (taskLoop_.isInLoopThread()) {
            stage_.deactivated(stateContext_, state);
            return;
        }
        taskLoop_.offer(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                stage_.deactivated(stateContext_, state);
                return DONE;
            }
        });
    }

    void callCatchException(final Exception exception) {
        if (taskLoop_.isInLoopThread()) {
            stage_.exceptionCaught(stateContext_, exception);
            return;
        }
        taskLoop_.offer(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                stage_.exceptionCaught(stateContext_, exception);
                return DONE;
            }
        });
    }

    static class StoreContext implements StageContext {

        private PipelineElement base_;

        StoreContext(PipelineElement base) {
            base_ = base;
        }

        @Override
        public StageKey key() {
            return base_.key_;
        }

        @Override
        public Transport transport() {
            return base_.pipeline_.transport();
        }

        @Override
        public Object parameter() {
            return null;
        }

        @Override
        public void proceed(final Object message) {
            base_.next_.callStore(message);
        }

        @Override
        public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
            return base_.taskLoop_.schedule(task, timeout, timeUnit);
        }
    }

    static class LoadContext implements StageContext {

        private PipelineElement base_;

        LoadContext(PipelineElement base) {
            base_ = base;
        }

        @Override
        public StageKey key() {
            return base_.key_;
        }

        @Override
        public Transport transport() {
            return base_.pipeline_.transport();
        }

        @Override
        public Object parameter() {
            return null;
        }

        @Override
        public void proceed(final Object message) {
            base_.prev_.callLoad(message);
        }

        @Override
        public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
            return base_.taskLoop_.schedule(task, timeout, timeUnit);
        }
    }

    static class ParameterStoreStageContext implements StageContext {

        private final PipelineElement base_;
        private final Object parameter_;

        ParameterStoreStageContext(PipelineElement base, Object parameter) {
            base_ = base;
            parameter_ = parameter;
        }

        @Override
        public StageKey key() {
            return base_.key_;
        }

        @Override
        public Transport transport() {
            return base_.pipeline_.transport();
        }

        @Override
        public Object parameter() {
            return parameter_;
        }

        @Override
        public void proceed(final Object message) {
            base_.next_.callStore(message, parameter_);
        }

        @Override
        public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
            return base_.taskLoop_.schedule(task, timeout, timeUnit);
        }
    }

    static class ParameterLoadStageContext implements StageContext {

        private final PipelineElement base_;
        private final Object parameter_;

        ParameterLoadStageContext(PipelineElement base, Object parameter) {
            base_ = base;
            parameter_ = parameter;
        }

        @Override
        public StageKey key() {
            return base_.key_;
        }

        @Override
        public Transport transport() {
            return base_.pipeline_.transport();
        }

        @Override
        public Object parameter() {
            return parameter_;
        }

        @Override
        public void proceed(final Object message) {
            base_.prev_.callLoad(message, parameter());
        }

        @Override
        public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
            return base_.taskLoop_.schedule(task, timeout, timeUnit);
        }
    }

    static class StateContext implements StageContext {

        private final PipelineElement context_;

        StateContext(PipelineElement context) {
            context_ = context;
        }

        @Override
        public StageKey key() {
            return context_.key_;
        }

        @Override
        public Transport transport() {
            return context_.pipeline_.transport();
        }

        @Override
        public Object parameter() {
            return null;
        }

        @Override
        public void proceed(final Object message) {
        }

        @Override
        public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
            return context_.taskLoop_.schedule(task, timeout, timeUnit);
        }
    }

    private static class NullStage implements Stage {
        @Override
        public void stored(StageContext context, Object output) {
        }

        @Override
        public void loaded(StageContext context, Object input) {
        }

        @Override
        public void exceptionCaught(StageContext context, Exception exception) {
        }

        @Override
        public void activated(StageContext context) {
        }

        @Override
        public void deactivated(StageContext context, DeactivateState state) {
        }
    }

    private static class NullPipelineElementExecutorPool extends TaskLoopGroup<TaskLoop> {

        static final TaskFuture NULL_FUTURE = new TaskFuture(-1L, new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                return DONE;
            }
        });
        static final TaskLoop NULL_TASK_LOOP = new TaskLoop() {
            @Override
            protected void onOpen() {
            }
            @Override
            protected void onClose() {
            }
            @Override
            protected void poll(long timeout, TimeUnit timeUnit) throws Exception {
            }
            @Override
            protected void wakeUp() {
            }
            @Override
            public void offer(Task task) {
            }
            @Override
            public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
                return NULL_FUTURE;
            }
            @Override
            public void execute(Task task) {
            }
            @Override
            public boolean isInLoopThread() {
                return true;
            }
        };

        protected NullPipelineElementExecutorPool() {
            super(new NameCountThreadFactory("UNUSED0"), 1);
        }

        @Override
        public TaskLoop assign(TaskSelection context) {
            return NULL_TASK_LOOP;
        }

        @Override
        protected TaskLoop newTaskLoop() {
            return NULL_TASK_LOOP;
        }

        @Override
        public void close() {
        }
    }

    private static class NullPipeline implements Pipeline {
        @Override
        public Pipeline add(StageKey key, Stage stage) {
            return this;
        }

        @Override
        public Pipeline add(StageKey key, Stage stage, TaskLoopGroup<? extends TaskLoop> pool) {
            return this;
        }

        @Override
        public Pipeline addBefore(StageKey baseKey, StageKey key, Stage stage) {
            return this;
        }

        @Override
        public Pipeline addBefore(StageKey baseKey, StageKey key, Stage stage, TaskLoopGroup<? extends TaskLoop> pool) {
            return this;
        }

        @Override
        public Pipeline addAfter(StageKey baseKey, StageKey key, Stage stage) {
            return this;
        }

        @Override
        public Pipeline addAfter(StageKey baseKey, StageKey key, Stage stage, TaskLoopGroup<? extends TaskLoop> pool) {
            return this;
        }

        @Override
        public Pipeline remove(StageKey key) {
            return this;
        }

        @Override
        public Pipeline replace(StageKey oldKey, StageKey newKey, Stage newStage) {
            return this;
        }

        @Override
        public Pipeline replace(StageKey oldKey, StageKey newKey, Stage newStage, TaskLoopGroup<? extends TaskLoop> pool) {
            return this;
        }

        @Override
        public void store(Object message) {
        }

        @Override
        public void store(Object message, Object parameter) {
                    }

        @Override
        public void load(Object message) {
        }

        @Override
        public void load(Object message, Object parameter) {
        }

        @Override
        public void activate() {
        }

        @Override
        public void deactivate(DeactivateState state) {
        }

        @Override
        public void catchException(Exception exception) {
        }

        @Override
        public Transport transport() {
            return null;
        }

        @Override
        public String name() {
            return "NullPipeline";
        }

        @Override
        public PipelineElement searchElement(StageKey key) {
            return null;
        }
    }
}
