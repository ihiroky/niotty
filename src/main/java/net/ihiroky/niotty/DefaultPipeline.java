package net.ihiroky.niotty;

import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.Arguments;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A skeletal implementation of {@link Pipeline}.
 *
 */
public class DefaultPipeline implements Pipeline {

    private final String name_;
    private final AbstractTransport transport_;
    private final PipelineElement head_;
    private final PipelineElement tail_;
    private final EventDispatcherGroup eventDispatcherGroup_;

    private volatile boolean activated_;

    /**
     * Creates a new instance.
     *
     * @param name name of this pipeline
     * @param transport transport which associate with this pipeline
     * @param eventDispatcherGroup an pool which provides EventDispatcher to execute stages
     * @param tailStageKey a key to be associated with the tailStage
     * @param tailStage a stage that is executed at last in this pipeline
     */
    public DefaultPipeline(
            String name, AbstractTransport transport, EventDispatcherGroup eventDispatcherGroup,
            StageKey tailStageKey, Stage tailStage) {
        Arguments.requireNonNull(name, "name");
        Arguments.requireNonNull(transport, "transport");
        Arguments.requireNonNull(eventDispatcherGroup, "eventDispatcherGroup");
        Arguments.requireNonNull(tailStageKey, "tailStageKey");
        Arguments.requireNonNull(tailStage, "tailStage");

        transport_ = transport;
        PipelineElement tail = createContext(tailStageKey, tailStage, eventDispatcherGroup);
        PipelineElement head = PipelineElement.newNullObject();
        head.setNext(tail);

        name_ = name;
        head_ = head;
        tail_ = tail;
        eventDispatcherGroup_ = eventDispatcherGroup;
    }

    private static void addLink(PipelineElement prev, PipelineElement next, PipelineElement e) {
        e.setPrev(prev);
        e.setNext(next);
        prev.setNext(e);
        next.setPrev(e);
    }

    private static void removeLink(PipelineElement e) {
        PipelineElement prev = e.prev();
        PipelineElement next = e.next();
        prev.setNext(next);
        next.setPrev(prev);
        e.clearLink();
    }

    private static void replaceLink(PipelineElement oldElement, PipelineElement newElement) {
        PipelineElement prev = oldElement.prev();
        PipelineElement next = oldElement.next();
        newElement.setNext(next);
        newElement.setPrev(prev);
        prev.setNext(newElement);
        next.setPrev(newElement);
        oldElement.clearLink();
    }

    @Override
    public Pipeline add(StageKey key, Stage stage) {
        return add(key, stage, eventDispatcherGroup_);
    }

    @Override
    public Pipeline add(StageKey key, Stage stage, EventDispatcherGroup pool) {
        Arguments.requireNonNull(key, "key");
        Arguments.requireNonNull(stage, "stage");
        if (key.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must not be added.");
        }

        if (pool == null) {
            pool = eventDispatcherGroup_;
        }
        PipelineElement newContext;
        synchronized (head_) {
            if (head_.next() == tail_) {
                newContext = createContext(key, stage, pool);
                addLink(head_, tail_, newContext);
            } else {
                for (PipelineElement e = head_.next(); e.isValid(); e = e.next()) {
                    if (e.key().equals(key)) {
                        throw new IllegalArgumentException("key " + key + " already exists.");
                    }
                }
                newContext = createContext(key, stage, pool);
                addLink(tail_.prev(), tail_, newContext);
            }
        }
        if (activated_) {
            newContext.callActivated();
        }
        return this;
    }

    @Override
    public Pipeline addBefore(StageKey baseKey, StageKey key, Stage stage) {
        return addBefore(baseKey, key, stage, eventDispatcherGroup_);
    }

    @Override
    public Pipeline addBefore(StageKey baseKey, StageKey key, Stage stage, EventDispatcherGroup pool) {
        Arguments.requireNonNull(baseKey, "baseKey");
        Arguments.requireNonNull(key, "key");
        Arguments.requireNonNull(stage, "stage");
        if (key.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must not be added.");
        }

        if (pool == null) {
            pool = eventDispatcherGroup_;
        }

        boolean added = false;
        PipelineElement target = null;
        synchronized (head_) {
            for (PipelineElement e = tail_; e.isValid(); e = e.prev()) {
                StageKey ikey = e.key();
                if (ikey.equals(key)) {
                    throw new IllegalArgumentException("key " + key + " already exists.");
                }
                if (ikey.equals(baseKey)) {
                    target = e;
                }
            }
            if (target != null) {
                PipelineElement newContext = createContext(key, stage, pool);
                addLink(target.prev(), target, newContext);
                added = true;
            }
        }
        if (!added) {
            throw new NoSuchElementException("baseKey " + baseKey + " is not found.");
        }
        if (activated_) {
            target.callActivated();
        }
        return this;
    }

    @Override
    public Pipeline addAfter(StageKey baseKey, StageKey key, Stage stage) {
        return addAfter(baseKey, key, stage, eventDispatcherGroup_);
    }

    @Override
    public Pipeline addAfter(StageKey baseKey, StageKey key, Stage stage, EventDispatcherGroup pool) {
        Arguments.requireNonNull(baseKey, "baseKey");
        Arguments.requireNonNull(key, "key");
        Arguments.requireNonNull(stage, "stage");
        if (baseKey.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must be the tail of this pipeline.");
        }
        if (key.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must not be added.");
        }

        if (pool == null) {
            pool = eventDispatcherGroup_;
        }

        boolean added = false;
        PipelineElement newContext = null;
        synchronized (head_) {
            PipelineElement target = null;
            for (PipelineElement e = head_.next(); e.isValid(); e = e.next()) {
                StageKey ikey = e.key();
                if (ikey.equals(key)) {
                    throw new IllegalArgumentException("key " + key + " already exists.");
                }
                if (ikey.equals(baseKey)) {
                    target = e;
                }
            }
            if (target != null) {
                newContext = createContext(key, stage, pool);
                addLink(target, target.next(), newContext);
                added = true;
            }
        }
        if (!added) {
            throw new NoSuchElementException("baseKey " + baseKey + " is not found.");
        }
        if (activated_) {
            newContext.callActivated();
        }
        return this;
    }

    @Override
    public Pipeline remove(StageKey key) {
        Arguments.requireNonNull(key, "key");
        if (key.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must not be removed.");
        }

        PipelineElement removed = null;
        synchronized (head_) {
            for (PipelineElement e = head_.next(); e.isValid(); e = e.next()) {
                if (e.key().equals(key)) {
                    removeLink(e);
                    removed = e;
                    break;
                }
            }
        }
        if (removed == null) {
            throw new NoSuchElementException("key " + key + " is not found.");
        }
        if (activated_) {
            removed.callDeactivated();
        }
        removed.close();
        return this;
    }

    @Override
    public Pipeline replace(StageKey oldKey, StageKey newKey, Stage newStage) {
        return replace(oldKey, newKey, newStage, eventDispatcherGroup_);
    }

    @Override
    public Pipeline replace(StageKey oldKey, StageKey newKey, Stage newStage, EventDispatcherGroup pool) {
        Arguments.requireNonNull(oldKey, "oldKey");
        Arguments.requireNonNull(newKey, "newKey");
        Arguments.requireNonNull(newStage, "newStage");
        if (oldKey.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must not be removed.");
        }
        if (newKey.equals(IO_STAGE_KEY)) {
            throw new IllegalArgumentException(IO_STAGE_KEY + " must not be added.");
        }

        if (pool == null) {
            pool = eventDispatcherGroup_;
        }
        boolean replaced = false;
        PipelineElement target = null;
        PipelineElement newContext = null;
        synchronized (head_) {
            for (PipelineElement e = head_.next(); e.isValid(); e = e.next()) {
                StageKey ikey = e.key();
                if (ikey.equals(newKey)) {
                    throw new IllegalArgumentException("newKey " + newKey + " already exists.");
                }
                if (ikey.equals(oldKey)) {
                    target = e;
                }
            }
            if (target != null) {
                newContext = createContext(newKey, newStage, pool);
                replaceLink(target, newContext);
                replaced = true;
            }
        }
        if (!replaced) {
            throw new NoSuchElementException("oldKey " + oldKey + " is not found.");
        }
        if (activated_) {
            target.callDeactivated();
            newContext.callActivated();
        }
        target.close();
        return this;

    }

    @Override
    public String name() {
        return name_;
    }

    @Override
    public PipelineElement searchElement(StageKey key) {
        Arguments.requireNonNull(key, "key");
        for (PipelineElementIterator i = new PipelineElementIterator(head_); i.hasNext();) {
            PipelineElement e = i.next();
            if (e.key().equals(key)) {
                return e;
            }
        }
        throw new NoSuchElementException(key.toString());
    }

    /**
     * Creates a new context with the specified stage.
     * The implementation class should define this method as final and safe.
     * This method is called in the constructor.
     *
     * @param key the key to be associated with the stage
     * @param stage the stage
     * @param group a group of EventDispatcher which assigns EventDispatcher to execute the stage
     * @return the context
     */
    PipelineElement createContext(
            StageKey key, Stage stage, EventDispatcherGroup group) {
        return new PipelineElement(this, key, stage, group);
    }

    public void close() {
        for (PipelineElement ctx = head_.next(); ctx.isValid(); ctx = ctx.next()) {
            ctx.close();
        }
    }

    @Override
    public void store(Object message) {
        head_.next().callStore(message, null);
    }

    @Override
    public void store(Object message, Object parameter) {
        head_.next().callStore(message, parameter);
    }

    @Override
    public void load(CodecBuffer message, Object parameter) {
        tail_.callLoad(message, parameter);
    }

    @Override
    public void activate() {
        tail_.callActivated();
        activated_ = true;
    }

    @Override
    public void deactivate() {
        activated_ = false;
        tail_.callDeactivated();
    }

    @Override
    public void catchException(Exception exception) {
        tail_.callExceptionCaught(exception);
    }

    @Override
    public void eventTriggered(Object event) {
        tail_.callEventTriggered(event);
    }

    public AbstractTransport transport() {
        return transport_;
    }

    /**
     * <p>Returns a string representation of this pipeline.</p>
     * <p>The string representation consists of a list of the key and its stages.</p>
     * @return a string representation of this pipeline
     */
    public String toString() {
        StringBuilder b = new StringBuilder();
        final String separator = ", ";
        b.append('[');
        for (Iterator<PipelineElement> iterator = iterator(); iterator.hasNext();) {
            PipelineElement e = iterator.next();
            b.append('(');
            b.append(e.key()).append('=').append(e.stage());
            b.append(')');
            b.append(separator);
        }
        int length = b.length();
        if (length > 1) {
            b.delete(length - separator.length(), length);
        }
        b.append(']');
        return b.toString();
    }

    protected Iterator<PipelineElement> iterator() {
        return new PipelineElementIterator(head_);
    }

    /**
     * Iterates {@code PipelineElement} chain from head context.
     */
    private static class PipelineElementIterator implements Iterator<PipelineElement> {

        private PipelineElement context_;
        private PipelineElement prev_;

        PipelineElementIterator(PipelineElement head) {
            context_ = head;
            prev_ = null;
        }

        @Override
        public boolean hasNext() {
            return context_.next().isValid();
        }

        @Override
        public PipelineElement next() {
            prev_ = context_;
            context_ = context_.next();
            if (!context_.isValid()) {
                throw new NoSuchElementException();
            }
            return context_;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        public PipelineElement prev() {
            return prev_;
        }
    }
}
