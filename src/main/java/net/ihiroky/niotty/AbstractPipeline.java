package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * A skeletal implementation of {@link Pipeline}.
 *
 * @param <S> the type of a stage
 * @param <L> the type of the TaskLoop which executes the stages by default
 */
public abstract class AbstractPipeline<S, L extends TaskLoop> implements Pipeline<S> {

    private final String name_;
    private final AbstractTransport<L> transport_;
    private final PipelineElement<Object, Object> head_;
    private final Tail<S> tail_;
    private final TaskLoopGroup<L> taskLoopGroup_;
    private Logger logger_ = LoggerFactory.getLogger(AbstractPipeline.class);

    static final NullPipelineElementExecutorPool NULL_POOL = new NullPipelineElementExecutorPool();
    static final PipelineElement<Object, Object> TERMINAL = new NullPipelineElement();

    private static final int INPUT_TYPE = 0;
    private static final int OUTPUT_TYPE = 1;

    /**
     * Creates a new instance.
     *
     * @param name name of this pipeline
     * @param transport transport which associate with this pipeline
     * @param taskLoopGroup an pool which provides TaskLoop to execute stages.
     */
    protected AbstractPipeline(
            String name, AbstractTransport<L> transport, TaskLoopGroup<L> taskLoopGroup) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(transport, "transport");
        Objects.requireNonNull(taskLoopGroup, "taskLoopGroup");

        transport_ = transport;

        Tail<S> tail = createTail(taskLoopGroup);
        tail.setNext(TERMINAL);
        PipelineElement<Object, Object> head = new NullPipelineElement();
        head.setNext(tail);

        name_ = name;
        head_ = head;
        tail_ = tail;
        taskLoopGroup_ = taskLoopGroup;
    }

    @Override
    public Pipeline<S> add(StageKey key, S stage) {
        return add(key, stage, taskLoopGroup_);
    }

    @Override
    public Pipeline<S> add(StageKey key, S stage, TaskLoopGroup<? extends TaskLoop> pool) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(stage, "stage");
        if (key.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must not be added.");
        }

        synchronized (head_) {
            if (head_.next() == tail_) {
                PipelineElement<Object, Object> newContext = createContext(key, stage, pool);
                head_.setNext(newContext);
                newContext.setNext(tail_);
                return this;
            } else {
                for (PipelineElementIterator i = new PipelineElementIterator(head_); i.hasNext();) {
                    PipelineElement<Object, Object> context = i.next();
                    if (context.key().equals(key)) {
                        throw new IllegalArgumentException("key " + key + " already exists.");
                    }
                    if (context.next() == tail_) {
                        PipelineElement<Object, Object> newContext = createContext(key, stage, pool);
                        newContext.setNext(tail_);
                        context.setNext(newContext);
                        break;
                    }
                }
            }
        }
        return this;
    }

    @Override
    public Pipeline<S> addBefore(StageKey baseKey, StageKey key, S stage) {
        return addBefore(baseKey, key, stage, taskLoopGroup_);
    }

    @Override
    public Pipeline<S> addBefore(StageKey baseKey, StageKey key, S stage, TaskLoopGroup<? extends TaskLoop> pool) {
        Objects.requireNonNull(baseKey, "baseKey");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(stage, "stage");
        if (key.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must not be added.");
        }

        synchronized (head_) {
            PipelineElement<Object, Object> prev = null;
            PipelineElement<Object, Object> target = null;
            for (PipelineElementIterator i = new PipelineElementIterator(head_); i.hasNext();) {
                PipelineElement<Object, Object> context = i.next();
                StageKey ikey = context.key();
                if (ikey.equals(key)) {
                    throw new IllegalArgumentException("key " + key + " already exists.");
                }
                if (ikey.equals(baseKey)) {
                    prev = i.prev();
                    target = context;
                }
            }
            if (target != null) {
                PipelineElement<Object, Object> newContext = createContext(key, stage, pool);
                newContext.setNext(target);
                prev.setNext(newContext);
                return this;
            }
        }
        throw new NoSuchElementException("baseKey " + baseKey + " is not found.");
    }

    @Override
    public Pipeline<S> addAfter(StageKey baseKey, StageKey key, S stage) {
        return addAfter(baseKey, key, stage, taskLoopGroup_);
    }

    @Override
    public Pipeline<S> addAfter(StageKey baseKey, StageKey key, S stage, TaskLoopGroup<? extends TaskLoop> pool) {
        Objects.requireNonNull(baseKey, "baseKey");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(stage, "stage");
        if (baseKey.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must be the tail of this pipeline.");
        }
        if (key.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must not be added.");
        }

        synchronized (head_) {
            PipelineElement<Object, Object> target = null;
            for (PipelineElementIterator i = new PipelineElementIterator(head_); i.hasNext();) {
                PipelineElement<Object, Object> context = i.next();
                StageKey ikey = context.key();
                if (ikey.equals(key)) {
                    throw new IllegalArgumentException("key " + key + " already exists.");
                }
                if (ikey.equals(baseKey)) {
                    target = context;
                }
            }
            if (target != null) {
                PipelineElement<Object, Object> next = target.next();
                PipelineElement<Object, Object> newContext = createContext(key, stage, pool);
                newContext.setNext(next);
                target.setNext(newContext);
                return this;
            }
        }
        throw new NoSuchElementException("baseKey " + baseKey + " is not found.");
    }

    @Override
    public Pipeline<S> remove(StageKey key) {
        Objects.requireNonNull(key, "key");
        if (key.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must not be removed.");
        }

        synchronized (head_) {
            for (PipelineElementIterator i = new PipelineElementIterator(head_); i.hasNext();) {
                PipelineElement<Object, Object> context = i.next();
                if (context.key().equals(key)) {
                    PipelineElement<Object, Object> prev = i.prev();
                    prev.setNext(context.next());
                    context.close();
                    return this;
                }
            }
        }
        throw new NoSuchElementException("key " + key + " is not found.");
    }

    @Override
    public Pipeline<S> replace(StageKey oldKey, StageKey newKey, S newStage) {
        return replace(oldKey, newKey, newStage, taskLoopGroup_);
    }

    @Override
    public Pipeline<S> replace(StageKey oldKey, StageKey newKey, S newStage, TaskLoopGroup<? extends TaskLoop> pool) {
        Objects.requireNonNull(oldKey, "oldKey");
        Objects.requireNonNull(newKey, "newKey");
        Objects.requireNonNull(newStage, "newStage");
        if (oldKey.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must not be removed.");
        }
        if (newKey.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must not be added.");
        }

        synchronized (head_) {
            PipelineElement<Object, Object> prev = null;
            PipelineElement<Object, Object> target = null;
            for (PipelineElementIterator i = new PipelineElementIterator(head_); i.hasNext();) {
                PipelineElement<Object, Object> context = i.next();
                StageKey ikey = context.key();
                if (ikey.equals(newKey)) {
                    throw new IllegalArgumentException("newKey " + newKey + " already exists.");
                }
                if (ikey.equals(oldKey)) {
                    prev = i.prev();
                    target = context;
                }
            }
            if (target != null) {
                PipelineElement<Object, Object> next = target.next();

                PipelineElement<Object, Object> newContext = createContext(newKey, newStage, pool);
                newContext.setNext(next);
                prev.setNext(newContext);
                target.close();
                return this;
            }
        }
        throw new NoSuchElementException("oldKey " + oldKey + " is not found.");
    }

    @Override
    public String name() {
        return name_;
    }

    @Override
    public StageContext<Object> searchContext(StageKey key) {
        Objects.requireNonNull(key, "key");
        for (Iterator<PipelineElement<Object, Object>> i = new PipelineElementIterator(head_); i.hasNext();) {
            PipelineElement<Object, Object> e = i.next();
            if (e.key().equals(key)) {
                return e;
            }
        }
        throw new NoSuchElementException(key.toString());
    }

    protected abstract PipelineElement<Object, Object> createContext(
            StageKey key, S stage, TaskLoopGroup<? extends TaskLoop> pool);

    protected abstract Tail<S> createTail(TaskLoopGroup<L> defaultPool);

    void setTailStage(S stage) {
        Objects.requireNonNull(stage, "stage");
        logger_.debug("[setTailStage] {}", stage);
        tail_.setStage(stage);
    }

    public void close() {
        for (PipelineElement<Object, Object> ctx = head_; ctx != TERMINAL; ctx = ctx.next()) {
            ctx.close();
        }
    }

    public void verifyStageType() {
        // TODO review if a dedicated flag instead of log level should be used.
        if (!logger_.isWarnEnabled()) {
            return;
        }

        int counter = 0;
        Class<?> prevOutputClass = null;
        Class<?> prevStageClass = null;
        for (PipelineElementIterator i = new PipelineElementIterator(head_); i.hasNext();) {
            PipelineElement<Object, Object> e = i.next();
            Object stage = e.stage();
            if (stage == null) {
                if (e instanceof Tail) {
                    logger_.debug("[verifyStageType] The tail stage: {}", stage);
                } else {
                    logger_.debug("[verifyStageType] The stage of {} is null.", e);
                }
                continue;
            }
            Class<?> stageClass = stage.getClass();
            for (Type type : stageClass.getGenericInterfaces()) {
                Type[] actualTypeArguments = stageTypeParameters(type);
                if (actualTypeArguments == null) {
                    continue;
                }

                logger_.debug("[verifyStageType] {}:{} - I:{}, O:{}",
                        name_, counter, actualTypeArguments[INPUT_TYPE], actualTypeArguments[OUTPUT_TYPE]);

                checkIfStageTypeIsValid(stageClass, actualTypeArguments[INPUT_TYPE], prevStageClass, prevOutputClass);

                // Update previous stage and output type.
                if (actualTypeArguments[OUTPUT_TYPE] instanceof Class) {
                    prevOutputClass = (Class<?>) actualTypeArguments[OUTPUT_TYPE];
                    prevStageClass = stageClass;
                } else {
                    logger_.debug(
                            "[verifyStageType] output type {} of {} is not an instance of Class.",
                            actualTypeArguments[OUTPUT_TYPE], stageClass);
                    prevOutputClass = null;
                    prevStageClass = null;
                }
                counter++;
                break;
            }
        }
    }

    /**
     * Returns type parameters of a specified {@code type} if the {@code type} is {@link net.ihiroky.niotty.LoadStage}
     * or {@link net.ihiroky.niotty.StoreStage}. Otherwise, returns null.
     *
     * @param type a type of the stage generic interface
     * @return type parameters or null
     */
    private Type[] stageTypeParameters(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type rawType = parameterizedType.getRawType();
        if (!(rawType instanceof Class)) {
            return null;
        }
        Class<?> rawTypeClass = (Class<?>) rawType;
        if (!(rawTypeClass.equals(LoadStage.class) || rawTypeClass.equals(StoreStage.class))) {
            return null;
        }
        return parameterizedType.getActualTypeArguments();
    }

    /**
     * Check if the input type of the stage is assignable from the previous output type of the previous stage.
     * That is, the stage is called by the previous stage without ClassCastException.
     *
     * @param stageClass class of the stage
     * @param inputType input type of the stage
     * @param prevStageClass class of the previous stage
     * @param prevOutputClass output type of the previous stage
     * @throws java.lang.RuntimeException the previous stage can't call the stage because of type mismatch.
     */
    private void checkIfStageTypeIsValid(
            Class<?> stageClass, Type inputType, Class<?> prevStageClass, Class<?> prevOutputClass) {
        if (inputType instanceof Class) {
            Class<?> inputClass = (Class<?>) inputType;
            if (prevOutputClass != null) {
                if (inputClass.isAssignableFrom(prevOutputClass)) {
                    logger_.debug("[checkIfStageTypeIsValid] OK from [{}] to [{}]", prevStageClass, stageClass);
                } else {
                    logger_.warn("Input type [{}] of [{}] is not assignable from output type [{}] of [{}].",
                            inputClass, stageClass, prevOutputClass, prevStageClass);
                }
            }
        } else {
            logger_.debug(
                    "[checkIfStageTypeIsValid] input type {} of {} is not an instance of Class. Skip assignment check.",
                    inputType, stageClass);
        }
    }

    public void execute(final Object input) {
        final PipelineElement<Object, Object> next = head_.next();
        next.taskLoop().execute(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                next.fire(input);
                return DONE;
            }
        });
    }

    public void execute(final Object input, final TransportParameter parameter) {
        final PipelineElement<Object, Object> next = head_.next();
        next.taskLoop().execute(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                next.fire(input, parameter);
                return DONE;
            }
        });
    }

    public void execute(final TransportStateEvent event) {
        final PipelineElement<?, ?> next = head_.next();
        next.taskLoop().execute(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                next.fire(event);
                next.proceed(event);
                return DONE;
            }
        });

    }

    AbstractTransport<L> transport() {
        return transport_;
    }

    protected Iterator<PipelineElement<Object, Object>> iterator() {
        return new PipelineElementIterator(head_);
    }

    private static class NullPipelineElement extends PipelineElement<Object, Object> {
        protected NullPipelineElement() {
            super(NullPipelineElementExecutorPool.NULL_TASK_LOOP);
        }
        @Override
        protected Object stage() {
            return this;
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
        @Override
        public void close() {
        }
        @Override
        public void proceed(final Object output) {
        }
        @Override
        protected void proceed(final Object output, final TransportParameter parameter) {
        }
        @Override
        protected void proceed(final TransportStateEvent event) {
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
        };

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

    /**
     * Iterates {@code PipelineElement} chain from head context.
     */
    private static class PipelineElementIterator implements Iterator<PipelineElement<Object, Object>> {

        private PipelineElement<Object, Object> context_;
        private final PipelineElement<Object, Object> terminal_;
        private PipelineElement<Object, Object> prev_;

        PipelineElementIterator(PipelineElement<Object, Object> head) {
            this(head, TERMINAL);
        }

        PipelineElementIterator(PipelineElement<Object, Object> head, PipelineElement<Object, Object> terminal) {
            context_ = head;
            terminal_ = terminal;
            prev_ = null;
        }

        @Override
        public boolean hasNext() {
            return context_.next() != terminal_;
        }

        @Override
        public PipelineElement<Object, Object> next() {
            prev_ = context_;
            context_ = context_.next();
            if (context_ == terminal_) {
                throw new NoSuchElementException();
            }
            return context_;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        public PipelineElement<Object, Object> prev() {
            return prev_;
        }
    }

    static abstract class Tail<S> extends PipelineElement<Object, Object> {

        protected Tail(AbstractPipeline<?, ?> pipeline, StageKey key, TaskLoopGroup<? extends TaskLoop> pool) {
            super(pipeline, key, pool);
        }

        abstract void setStage(S stage);
    }
}
