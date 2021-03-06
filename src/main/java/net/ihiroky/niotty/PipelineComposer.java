package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Closable;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Composes a pipeline which is managed by an implementation of {@link Transport}.</p>
 *
 * <p>{@link #compose(Pipeline)} composes load and store pipelines by adding {@link Stage}s.</p>
 *
 * <h3>Set up and close for support objects.</h3>
 * <p>This class has life cycle methods {@link #setUp()} and {@link #close()},
 * and a subsidiary method {@link #addClosable(Closable)} . If it is necessary
 * to set up support objects like {@link DefaultEventDispatcherGroup} to execute {@link Stage},
 * and {@link net.ihiroky.niotty.buffer.ChunkPool}, which are used
 * over the composing plural sets of load and store pipelines, {@link #setUp()} is overridden
 * and the objects is initialized in it. If tear down, {@link #close()} is overridden.
 * If the objects implements {@link net.ihiroky.niotty.util.Closable},
 * it can be used to tear down the objects. The default implementation of {@link #close()} calls these
 * {@link net.ihiroky.niotty.util.Closable#close()}. So {@link #close()} should not be
 * overridden or should be called by sub class if {@link #addClosable(Closable)}
 * is used.</p>
 */
public abstract class PipelineComposer {

    private final List<Closable> closeableList_;

    private static final int INITIAL_CAPACITY = 3;

    private static final PipelineComposer EMPTY = new PipelineComposer(0) {
        @Override
        protected void addClosable(Closable closeable) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void compose(Pipeline pipeline) {
        }
    };

    /**
     * Returns a pipeline composer which compose nothing.
     * @return a pipeline composer which compose nothing.
     */
    public static PipelineComposer empty() {
        return EMPTY;
    }

    /**
     * <p>Constructs a new instance.</p>
     *
     * <p>The size of area to hold the objects be added by {@link #addClosable(Closable)} is set to 3</p>.
     */
    protected PipelineComposer() {
        this(INITIAL_CAPACITY);
    }

    /**
     * <p>Constructs a new instance.</p>
     *
     * <p>The size of area to hold the objects be added by {@link #addClosable(Closable)} is set to
     * {@code initialCapacity}.</p>.
     *
     * @param initialCapacity the initial size of array, which holds the support objects.
     *                        If there is no need to use {@link #addClosable(net.ihiroky.niotty.util.Closable)},
     *                        0 is recommended.
     */
    protected PipelineComposer(int initialCapacity) {
        closeableList_ = new ArrayList<Closable>(initialCapacity);
    }

    /**
     * <p>Adds the support objects to close these objects with proper timing.</p>
     *
     * <p>They are closed by the default implementation of {@link #close()}. If this method is used,
     * {@link #close()} should not be overridden or should be called by sub class.</p>
     * @param closable the objects which is closed with proper timing.
     */
    protected void addClosable(Closable closable) {
        synchronized (closeableList_) {
            closeableList_.add(closable);
        }
    }

    /**
     * <p>Initializes the support objects.</p>
     *
     * <p>They should be initialized in this method if necessary.</p>
     */
    public void setUp() {
    }

    /**
     * <p>Closes the support objects.</p>
     *
     * <p>The default implementation of this method calls {@code} methods for the support objects
     * added by {@link #addClosable(net.ihiroky.niotty.util.Closable)}. So this method should not be
     * overridden or should be called by sub class if {@link #addClosable(net.ihiroky.niotty.util.Closable)}
     * is used.</p>
     */
    public void close() {
        synchronized (closeableList_) {
            for (Closable closable : closeableList_) {
                try {
                    closable.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            closeableList_.clear();
        }
    }

    /**
     * <p>composes the pipeline by adding, removing and replacing stages.</p>
     *
     * @param pipeline the pipeline.
     */
    public abstract void compose(Pipeline pipeline);
}
