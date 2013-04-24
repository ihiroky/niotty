package net.ihiroky.niotty;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 13/01/09, 18:57
 *
 * @author Hiroki Itoh
 */
public abstract class PipelineComposer {

    private List<Closeable> closeableList_;

    private static final int INITIAL_CAPACITY = 3;

    protected PipelineComposer() {
        closeableList_ = new ArrayList<>(INITIAL_CAPACITY);
    }

    protected void addCloseable(Closeable closeable) {
        closeableList_.add(closeable);
    }

    public void close() {
        for (Closeable closeable : closeableList_) {
            try {
                closeable.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public abstract void compose(LoadPipeline loadPipeline, StorePipeline storePipeline);
}
