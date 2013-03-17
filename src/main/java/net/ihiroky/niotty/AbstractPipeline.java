package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Created on 13/01/10, 17:21
 *
 * @author Hiroki Itoh
 */
public abstract class AbstractPipeline<S> implements Pipeline<S> {

    private final String name_;
    private final Transport transport_;
    private final StageContext<Object, Object> head_;
    private static Logger logger_ = LoggerFactory.getLogger(AbstractPipeline.class);

    private static final StageContext<Object, Object> TERMINAL = new NullContext();

    protected AbstractPipeline(String name, Transport transport) {
        StageContext<Object, Object> head = new NullContext();
        head.setNext(TERMINAL);

        name_ = name;
        transport_ = transport;
        head_ = head;
    }

    protected Pipeline<S> add(StageContext<Object, Object> newContext) {
        Objects.requireNonNull(newContext, "newContext");

        if (head_.next() == TERMINAL) {
            head_.setNext(newContext);
            newContext.setNext(TERMINAL);
            return this;
        } else {
            for (StageContextIterator i = new StageContextIterator(head_); i.hasNext();) {
                StageContext<Object, Object> context = i.next();
                if (context.next() == TERMINAL) {
                    newContext.setNext(TERMINAL);
                    context.setNext(newContext);
                    break;
                }
            }
        }
        return this;
    }

    @Override
    public Pipeline<S> add(StageKey key, S stage) {
        return add(key, stage, null);
    }

    @Override
    public Pipeline<S> add(StageKey key, S stage, StageContextExecutorPool pool) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(stage, "stage");

        if (head_.next() == TERMINAL) {
            StageContext<Object, Object> newContext = createContext(key, stage, pool);
            head_.setNext(newContext);
            newContext.setNext(TERMINAL);
            return this;
        } else {
            for (StageContextIterator i = new StageContextIterator(head_); i.hasNext();) {
                StageContext<Object, Object> context = i.next();
                if (context.next() == TERMINAL) {
                    StageContext<Object, Object> newContext = createContext(key, stage, pool);
                    newContext.setNext(TERMINAL);
                    context.setNext(newContext);
                    break;
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
    public Pipeline<S> addBefore(StageKey baseKey, StageKey key, S stage, StageContextExecutorPool pool) {
        Objects.requireNonNull(baseKey, "baseKey");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(stage, "stage");

        for (StageContextIterator i = new StageContextIterator(head_); i.hasNext();) {
            StageContext<Object, Object> context = i.next();
            if (context.key().equals(baseKey)) {
                StageContext<Object, Object> prev = i.prev();
                StageContext<Object, Object> newContext = createContext(key, stage, pool);
                newContext.setNext(context);
                prev.setNext(newContext);
                return this;
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public Pipeline<S> addAfter(StageKey baseKey, StageKey key, S stage) {
        return addAfter(baseKey, key, stage, null);
    }

    @Override
    public Pipeline<S> addAfter(StageKey baseKey, StageKey key, S stage, StageContextExecutorPool pool) {
        Objects.requireNonNull(baseKey, "baseKey");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(stage, "stage");

        for (StageContextIterator i = new StageContextIterator(head_); i.hasNext();) {
            StageContext<Object, Object> context = i.next();
            if (context.key().equals(baseKey)) {
                StageContext<Object, Object> next = context.next();
                StageContext<Object, Object> newContext = createContext(key, stage, pool);
                newContext.setNext(next);
                context.setNext(newContext);
                return this;
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public Pipeline<S> remove(StageKey key) {
        Objects.requireNonNull(key, "key");

        for (StageContextIterator i = new StageContextIterator(head_); i.hasNext();) {
            StageContext<Object, Object> context = i.next();
            if (context.key().equals(key)) {
                // don't call context.setNext(TERMINAL) for iteration or execution.
                StageContext<Object, Object> prev = i.prev();
                prev.setNext(context.next());
                context.close();
                return this;
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public Pipeline<S> replace(StageKey oldKey, StageKey newKey, S newStage) {
        return replace(oldKey, newKey, newStage, null);
    }

    @Override
    public Pipeline<S> replace(StageKey oldKey, StageKey newKey, S newStage, StageContextExecutorPool pool) {
        Objects.requireNonNull(oldKey, "oldKey");
        Objects.requireNonNull(newKey, "newKey");
        Objects.requireNonNull(newStage, "newStage");

        for (StageContextIterator i = new StageContextIterator(head_); i.hasNext();) {
            StageContext<Object, Object> context = i.next();
            if (context.key().equals(oldKey)) {
                // don't call context.setNext(TERMINAL) for iteration or execution.
                StageContext<Object, Object> prev = i.prev();
                StageContext<Object, Object> next = context.next();
                StageContext<Object, Object> newContext = createContext(newKey, newStage, pool);
                newContext.setNext(next);
                prev.setNext(newContext);
                context.close();
                return this;
            }
        }
        throw new NoSuchElementException();
    }

    protected abstract StageContext<Object, Object> createContext(StageKey key, S stage, StageContextExecutorPool pool);

    public void close() {
        for (StageContext<Object, Object> ctx = head_; ctx != TERMINAL; ctx = ctx.next()) {
            ctx.close();
        }
    }

    public void verifyStageContextType() {

        // TODO verify next input class is assignable from previous output class

        if (logger_.isDebugEnabled()) {
            int counter = 0;
            for (StageContextIterator i = new StageContextIterator(head_); i.hasNext();) {
                StageContext<Object, Object> ctx = i.next();
                for (Type type : ctx.stage().getClass().getGenericInterfaces()) {
                    if (type instanceof ParameterizedType) {
                        Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                        logger_.debug("[verifyStageContextType] {}:{} - I:{}, O:{}",
                                name_, counter++, actualTypeArguments[0], actualTypeArguments[1]);
                        break;
                    }
                }
            }
        }
    }

    public void execute(Object input) {
        head_.next().execute(input);
    }

    public void execute(TransportStateEvent event) {
        head_.next().execute(event);
    }

    @Override
    public String name() {
        return name_;
    }

    @Override
    public Transport transport() {
        return transport_;
    }

    protected StageContext<Object, Object> search(StageKey key) {
        for (StageContextIterator i = new StageContextIterator(head_); i.hasNext();) {
            StageContext<Object, Object> context = i.next();
            if (context.key() == key) {
                return context;
            }
        }
        return null;
    }

    protected Iterator<StageContext<Object, Object>> iterator() {
        return new StageContextIterator(head_);
    }

    private static class NullContext extends StageContext<Object, Object> {
        protected NullContext() {
            super(null, null, null);
        }
        @Override
        protected Object stage() {
            return this;
        }
        @Override
        protected void fire(Object input) {
        }
        @Override
        protected void fire(TransportStateEvent event) {
        }
    }

    /**
     * Iterates {@code StageContext} chain from head context.
     */
    private static class StageContextIterator implements Iterator<StageContext<Object, Object>> {

        private StageContext<Object, Object> context_;
        private final StageContext<Object, Object> terminal_;
        private StageContext<Object, Object> prev_;

        StageContextIterator(StageContext<Object, Object> head) {
            this(head, TERMINAL);
        }

        StageContextIterator(StageContext<Object, Object> head, StageContext<Object, Object> terminal) {
            context_ = head;
            terminal_ = terminal;
            prev_ = null;
        }

        @Override
        public boolean hasNext() {
            return context_.next() != terminal_;
        }

        @Override
        public StageContext<Object, Object> next() {
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

        public StageContext<Object, Object> prev() {
            return prev_;
        }
    }
}
