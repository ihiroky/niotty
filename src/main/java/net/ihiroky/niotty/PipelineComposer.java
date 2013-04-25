package net.ihiroky.niotty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 13/01/09, 18:57
 *
 * @author Hiroki Itoh
 */
public abstract class PipelineComposer {

    private List<AutoCloseable> closeableList_;

    private static final int INITIAL_CAPACITY = 3;

    private static final PipelineComposer EMPTY = new PipelineComposer(0) {
        @Override
        protected void addCloseable(AutoCloseable closeable) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
        }
    };

    public static PipelineComposer empty() {
        return EMPTY;
    }

    protected PipelineComposer() {
        this(INITIAL_CAPACITY);
    }

    protected PipelineComposer(int initialCapacity) {
        closeableList_ = new ArrayList<>(initialCapacity);
    }

    protected void addCloseable(AutoCloseable closeable) {
        closeableList_.add(closeable);
    }

    public void close() {
        for (AutoCloseable closeable : closeableList_) {
            try {
                closeable.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public abstract void compose(LoadPipeline loadPipeline, StorePipeline storePipeline);
}
