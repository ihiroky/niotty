package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TaskTimer;

/**
 * @author Hiroki Itoh
 */
public class TcpIOSelectorPool extends AbstractSelectorPool<TcpIOSelector> {

    private int readBufferSize_;
    private boolean direct_;
    private TaskTimer taskTimer_;

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    public TcpIOSelectorPool() {
        readBufferSize_ = DEFAULT_BUFFER_SIZE;
        direct_ = false;
    }

    public void setReadBufferSize(int size) {
        if (readBufferSize_ <= 0) {
            throw new IllegalArgumentException("readBufferSize must be positive.");
        }
        readBufferSize_ = size;
    }

    public void setDirect(boolean direct) {
        direct_ = direct;
    }

    public void setTaskTimer(TaskTimer taskTimer) {
        taskTimer_ = taskTimer;
    }

    @Override
    protected TcpIOSelector newTaskLoop() {
        return new TcpIOSelector(taskTimer_, readBufferSize_, direct_);
    }
}
