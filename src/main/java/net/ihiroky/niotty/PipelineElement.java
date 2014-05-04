package net.ihiroky.niotty;

import net.ihiroky.niotty.buffer.CodecBuffer;
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
    final EventDispatcher eventDispatcher_;
    final StoreContext storeContext_;
    final LoadContext loadContext_;
    final StateContext stateContext_;
    final int stageType_;

    private static final Pipeline NULL_PIPELINE = new NullPipeline();
    private static final StageKey NULL_STAGE_KEY = StageKeys.of("NullStage");
    private static final Stage NULL_STAGE = new NullStage();
    private static final EventDispatcherGroup<EventDispatcher> NULL_EVENT_DISPATCHER_GROUP = new NullPipelineElementExecutorPool();
    private static final PipelineElement TERMINAL = newNullObject();
    private static final int STAGE_TYPE_BOTH = 0;
    private static final int STAGE_TYPE_STORE = 1;
    private static final int STAGE_TYPE_LOAD = 2;

    protected PipelineElement(Pipeline pipeline, StageKey key, Stage stage,
            EventDispatcherGroup<? extends EventDispatcher> pool) {
        pipeline_ = Arguments.requireNonNull(pipeline, "pipeline");
        key_ = Arguments.requireNonNull(key, "key");
        stage_ = Arguments.requireNonNull(stage, "stage");
        eventDispatcher_ = Arguments.requireNonNull(pool, "pool").assign(pipeline.transport());
        storeContext_ = new StoreContext(this);
        loadContext_ = new LoadContext(this);
        stateContext_ = new StateContext(this);
        next_ = TERMINAL;
        prev_ = TERMINAL;
        if (stage instanceof StoreStage) {
            stageType_ = STAGE_TYPE_STORE;
        } else if (stage instanceof LoadStage) {
            stageType_ = STAGE_TYPE_LOAD;
        } else {
            stageType_ = STAGE_TYPE_BOTH;
        }

    }

    static PipelineElement newNullObject() {
        return new PipelineElement(NULL_PIPELINE, NULL_STAGE_KEY, NULL_STAGE, NULL_EVENT_DISPATCHER_GROUP);
    }

    static Stage newNullStage() {
        return new NullStage();
    }

    public StageKey key() {
        return key_;
    }

    public void execute(Event event) {
        eventDispatcher_.execute(event);
    }

    public EventFuture schedule(Event event, long timeout, TimeUnit timeUnit) {
        return eventDispatcher_.schedule(event, timeout, timeUnit);
    }

    public Stage stage() {
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
        eventDispatcher_.reject(pipeline_.transport());
    }


    void callStore(final Object message, final Object parameter) {
        // Reduce to switch the threads if the stages side by side use the different threads.
        if (stageType_ == STAGE_TYPE_LOAD || eventDispatcher_.isInDispatcherThread()) {
            stage_.stored(storeContext_, message, parameter);
        } else {
            eventDispatcher_.offer(new Event() {
                @Override
                public long execute() throws Exception {
                    stage_.stored(storeContext_, message, parameter);
                    return DONE;
                }
            });
        }
    }

    // expand to context class
    void callLoad(final Object message, final Object parameter) {
        if (stageType_ == STAGE_TYPE_STORE || eventDispatcher_.isInDispatcherThread()) {
            stage_.loaded(loadContext_, message, parameter);
        } else {
            eventDispatcher_.offer(new Event() {
                @Override
                public long execute() throws Exception {
                    stage_.loaded(loadContext_, message, parameter);
                    return DONE;
                }
            });
        }
    }

    void callActivate() {
        if (eventDispatcher_.isInDispatcherThread()) {
            stage_.activated(stateContext_);
        } else {
            eventDispatcher_.offer(new Event() {
            @Override
            public long execute() throws Exception {
                stage_.activated(stateContext_);
                return DONE;
            }
            });
        }
    }

    void callDeactivate() {
        if (eventDispatcher_.isInDispatcherThread()) {
            stage_.deactivated(stateContext_);
        } else {
            eventDispatcher_.offer(new Event() {
            @Override
            public long execute() throws Exception {
                stage_.deactivated(stateContext_);
                return DONE;
            }
            });
        }
    }

    void callCatchException(final Exception exception) {
        if (eventDispatcher_.isInDispatcherThread()) {
            stage_.exceptionCaught(stateContext_, exception);
        } else {
            eventDispatcher_.offer(new Event() {
            @Override
            public long execute() throws Exception {
                stage_.exceptionCaught(stateContext_, exception);
                return DONE;
            }
            });
        }
    }

    void callEventTriggered(final Object event) {
        if (eventDispatcher_.isInDispatcherThread()) {
            stage_.eventTriggered(stateContext_, event);
        } else {
            eventDispatcher_.offer(new Event() {
                @Override
                public long execute() throws Exception {
                    stage_.eventTriggered(stateContext_, event);
                    return DONE;
                }
            });
        }
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
        public void proceed(Object message, Object parameter) {
            base_.next_.callStore(message, parameter);
        }

        @Override
        public EventFuture schedule(Event event, long timeout, TimeUnit timeUnit) {
            return base_.eventDispatcher_.schedule(event, timeout, timeUnit);
        }

        @Override
        public boolean changesDispatcherOnProceed() {
            return base_.next_.eventDispatcher_.isInDispatcherThread();
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
        public void proceed(Object message, Object parameter) {
            base_.prev_.callLoad(message, parameter);
        }

        @Override
        public EventFuture schedule(Event event, long timeout, TimeUnit timeUnit) {
            return base_.eventDispatcher_.schedule(event, timeout, timeUnit);
        }

        @Override
        public boolean changesDispatcherOnProceed() {
            return base_.prev_.eventDispatcher_.isInDispatcherThread();
        }
    }

    static class StateContext implements StageContext {

        private final PipelineElement base_;

        StateContext(PipelineElement context) {
            base_ = context;
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
        public void proceed(Object message, Object parameter) {
        }

        @Override
        public EventFuture schedule(Event event, long timeout, TimeUnit timeUnit) {
            return base_.eventDispatcher_.schedule(event, timeout, timeUnit);
        }

        @Override
        public boolean changesDispatcherOnProceed() {
            throw new UnsupportedOperationException(
                    "The changesDispatcherOnProceed is supported only on load()/store().");
        }
    }

    private static class NullStage implements Stage {
        @Override
        public void stored(StageContext context, Object output, Object parameter) {
        }

        @Override
        public void loaded(StageContext context, Object input, Object parameter) {
        }

        @Override
        public void exceptionCaught(StageContext context, Exception exception) {
        }

        @Override
        public void activated(StageContext context) {
        }

        @Override
        public void deactivated(StageContext context) {
        }

        @Override
        public void eventTriggered(StageContext context, Object event) {
        }
    }

    private static class NullPipelineElementExecutorPool extends EventDispatcherGroup<EventDispatcher> {

        static final EventFuture NULL_FUTURE = new EventFuture(-1L, new Event() {
            @Override
            public long execute() throws Exception {
                return DONE;
            }
        });
        static final EventDispatcher NULL_EVENT_DISPATCHER = new EventDispatcher() {
            @Override
            protected void onOpen() {
            }
            @Override
            protected void onClose() {
            }
            @Override
            protected void poll(long timeoutNanos) throws Exception {
            }
            @Override
            protected void wakeUp() {
            }
            @Override
            public void offer(Event event) {
            }
            @Override
            public EventFuture schedule(Event event, long timeout, TimeUnit timeUnit) {
                return NULL_FUTURE;
            }
            @Override
            public void execute(Event event) {
            }
            @Override
            public boolean isInDispatcherThread() {
                return true;
            }
        };

        protected NullPipelineElementExecutorPool() {
            super(1, new NameCountThreadFactory("UNUSED0"), new EventDispatcherFactory<EventDispatcher>() {
                @Override
                public EventDispatcher newEventDispatcher() {
                    return NULL_EVENT_DISPATCHER;
                }
            });
        }

        @Override
        public EventDispatcher assign(EventDispatcherSelection context) {
            return NULL_EVENT_DISPATCHER;
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
        public Pipeline add(StageKey key, Stage stage, EventDispatcherGroup<? extends EventDispatcher> pool) {
            return this;
        }

        @Override
        public Pipeline addBefore(StageKey baseKey, StageKey key, Stage stage) {
            return this;
        }

        @Override
        public Pipeline addBefore(StageKey baseKey, StageKey key, Stage stage, EventDispatcherGroup<? extends EventDispatcher> pool) {
            return this;
        }

        @Override
        public Pipeline addAfter(StageKey baseKey, StageKey key, Stage stage) {
            return this;
        }

        @Override
        public Pipeline addAfter(StageKey baseKey, StageKey key, Stage stage, EventDispatcherGroup<? extends EventDispatcher> pool) {
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
        public Pipeline replace(StageKey oldKey, StageKey newKey, Stage newStage, EventDispatcherGroup<? extends EventDispatcher> pool) {
            return this;
        }

        @Override
        public void store(Object message) {
        }

        @Override
        public void store(Object message, Object parameter) {
        }

        @Override
        public void load(CodecBuffer message, Object parameter) {
        }

        @Override
        public void activate() {
        }

        @Override
        public void deactivate() {
        }

        @Override
        public void catchException(Exception exception) {
        }

        @Override
        public void eventTriggered(Object event) {

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
