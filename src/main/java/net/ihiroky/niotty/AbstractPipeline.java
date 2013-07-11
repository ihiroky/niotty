package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A skeletal implemention of {@link Pipeline}.
 *
 * @author Hiroki Itoh
 */
public abstract class AbstractPipeline<S> implements Pipeline<S> {

    private final String name_;
    private final Transport transport_;
    private final PipelineElement<Object, Object> head_;
    private static Logger logger_ = LoggerFactory.getLogger(AbstractPipeline.class);

    private static final PipelineElement<Object, Object> TERMINAL = new NullPipelineElement();
    private static final int INPUT_TYPE = 0;
    private static final int OUTPUT_TYPE = 1;

    protected AbstractPipeline(String name, Transport transport) {
        PipelineElement<Object, Object> head = new NullPipelineElement();
        head.setNext(TERMINAL);

        name_ = name;
        transport_ = transport;
        head_ = head;
    }

    @Override
    public Pipeline<S> add(StageKey key, S stage) {
        return add(key, stage, null);
    }

    @Override
    public Pipeline<S> add(StageKey key, S stage, PipelineElementExecutorPool pool) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(stage, "stage");

        synchronized (head_) {
            if (head_.next() == TERMINAL) {
                PipelineElement<Object, Object> newContext = createContext(key, stage, pool);
                head_.setNext(newContext);
                newContext.setNext(TERMINAL);
                return this;
            } else {
                for (PipelineElementIterator i = new PipelineElementIterator(head_); i.hasNext();) {
                    PipelineElement<Object, Object> context = i.next();
                    if (context.key().equals(key)) {
                        throw new IllegalArgumentException("key " + key + " already exists.");
                    }
                    if (context.next() == TERMINAL) {
                        PipelineElement<Object, Object> newContext = createContext(key, stage, pool);
                        newContext.setNext(TERMINAL);
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
        return addBefore(baseKey, key, stage, null);
    }

    @Override
    public Pipeline<S> addBefore(StageKey baseKey, StageKey key, S stage, PipelineElementExecutorPool pool) {
        Objects.requireNonNull(baseKey, "baseKey");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(stage, "stage");

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
        return addAfter(baseKey, key, stage, null);
    }

    @Override
    public Pipeline<S> addAfter(StageKey baseKey, StageKey key, S stage, PipelineElementExecutorPool pool) {
        Objects.requireNonNull(baseKey, "baseKey");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(stage, "stage");

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
        return replace(oldKey, newKey, newStage, null);
    }

    @Override
    public Pipeline<S> replace(StageKey oldKey, StageKey newKey, S newStage, PipelineElementExecutorPool pool) {
        Objects.requireNonNull(oldKey, "oldKey");
        Objects.requireNonNull(newKey, "newKey");
        Objects.requireNonNull(newStage, "newStage");

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

    protected abstract PipelineElement<Object, Object> createContext(
            StageKey key, S stage, PipelineElementExecutorPool pool);

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
            Class<?> stageClass = i.next().stage().getClass();
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

    public void execute(Object input) {
        PipelineElement<Object, Object> next = head_.next();
        next.executor().execute(next, input);
    }

    public void execute(Object input, TransportParameter parameter) {
        PipelineElement<Object, Object> next = head_.next();
        next.executor().execute(next, input, parameter);
    }

    public void execute(TransportStateEvent event) {
        PipelineElement<?, ?> next = head_.next();
        next.executor().execute(next, event);
    }

    @Override
    public String name() {
        return name_;
    }

    @Override
    public Transport transport() {
        return transport_;
    }

    protected PipelineElement<Object, Object> search(StageKey key) {
        for (PipelineElementIterator i = new PipelineElementIterator(head_); i.hasNext();) {
            PipelineElement<Object, Object> context = i.next();
            if (context.key() == key) {
                return context;
            }
        }
        return null;
    }

    protected Iterator<PipelineElement<Object, Object>> iterator() {
        return new PipelineElementIterator(head_);
    }

    private static class NullPipelineElement extends PipelineElement<Object, Object> {
        protected NullPipelineElement() {
            super(null, null, NullPipelineElementExecutorPool.INSTANCE);
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
    }

    private static class NullPipelineElementExecutorPool implements PipelineElementExecutorPool {

        static final NullPipelineElementExecutorPool INSTANCE = new NullPipelineElementExecutorPool();

        @Override
        public PipelineElementExecutor assign(PipelineElement<?, ?> context) {
            return NullPipelineElementExecutor.INSTANCE;
        }

        @Override
        public void close() {
        }
    }

    private static class NullPipelineElementExecutor implements PipelineElementExecutor {

        static final NullPipelineElementExecutor INSTANCE = new NullPipelineElementExecutor();

        @Override
        public <I> void execute(PipelineElement<I, ?> context, I input) {
        }

        @Override
        public <I> void execute(PipelineElement<I, ?> context, I input, TransportParameter parameter) {
        }

        @Override
        public void execute(PipelineElement<?, ?> context, TransportStateEvent event) {
        }

        @Override
        public PipelineElementExecutorPool pool() {
            return NullPipelineElementExecutorPool.INSTANCE;
        }

        @Override
        public void close(PipelineElement<?, ?> context) {
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
}
