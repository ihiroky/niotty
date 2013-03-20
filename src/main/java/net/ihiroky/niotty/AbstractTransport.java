package net.ihiroky.niotty;

import net.ihiroky.niotty.buffer.BufferSink;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TODO invalidate pipeline on close and recreate pipelines.
 * @author Hiroki Itoh
 */
abstract public class AbstractTransport<L extends TaskLoop<L>> implements Transport {

    private DefaultLoadPipeline loadPipeline_;
    private DefaultStorePipeline storePipeline_;
    private AtomicReference<Object> attachmentReference_;
    private TransportListener transportListener_;
    private L loop_;

    private static final TransportListener NULL_LISTENER = new NullListener();
    private static final DefaultLoadPipeline NULL_LOAD_PIPELINE = new DefaultLoadPipeline("null", null);
    private static final DefaultStorePipeline NULL_STORE_PIPELINE = new DefaultStorePipeline("null", null);

    protected AbstractTransport() {
        loadPipeline_ = NULL_LOAD_PIPELINE;
        storePipeline_ = NULL_STORE_PIPELINE;
        attachmentReference_ = new AtomicReference<>();
        transportListener_ = NULL_LISTENER;
    }

    protected void setUpPipelines(String baseName, PipelineInitializer pipelineInitializer) {

        DefaultLoadPipeline loadPipeline = new DefaultLoadPipeline(baseName, this);
        DefaultStorePipeline storePipeline = new DefaultStorePipeline(baseName, this);
        pipelineInitializer.setUpPipeline(loadPipeline, storePipeline);

        loadPipeline.verifyStageType();
        storePipeline.verifyStageType();

        loadPipeline_ = loadPipeline;
        storePipeline_ = storePipeline;
    }

    protected void executeLoad(Object message) {
        loadPipeline_.execute(message);
    }

    protected void executeLoad(TransportStateEvent stateEvent) {
        loadPipeline_.execute(stateEvent);
    }

    protected void executeStore(Object message) {
        storePipeline_.execute(message);
    }

    protected void executeStore(TransportStateEvent stateEvent) {
        storePipeline_.execute(stateEvent);
    }

    public final void resetPipelines(PipelineInitializer initializer) {
        Objects.requireNonNull(initializer, "initializer");

        // use the same lock object as listener to save memory footprint.
        synchronized (this) {
            DefaultLoadPipeline oldLoadPipeline = loadPipeline_;
            DefaultStorePipeline oldStorePipeline = storePipeline_;
            DefaultLoadPipeline loadPipelineCopy = oldLoadPipeline.createCopy();
            DefaultStorePipeline storePipelineCopy = oldStorePipeline.createCopy();
            initializer.setUpPipeline(loadPipelineCopy, storePipelineCopy);

            StoreStage<BufferSink, Void> ioStage = oldStorePipeline.searchIOStage();
            if (ioStage != null) {
                storePipelineCopy.addIOStage(ioStage);
            }
            loadPipelineCopy.verifyStageType();
            storePipelineCopy.verifyStageType();

            loadPipeline_ = loadPipelineCopy;
            storePipeline_ = storePipelineCopy;
            oldLoadPipeline.close();
            oldStorePipeline.close();
        }
    }

    public final void closePipelines() {
        loadPipeline_.close();
        storePipeline_.close();
    }

    public final void addIOStage(StoreStage<BufferSink, Void> ioStage) {
        Objects.requireNonNull(ioStage, "ioStage");
        storePipeline_.addIOStage(ioStage);
    }

    public final void setEventLoop(L loop) {
        Objects.requireNonNull(loop, "loop");
        this.loop_ = loop;
    }

    protected final L getEventLoop() {
        return loop_;
    }

    public void offerTask(TaskLoop.Task<L> task) {
        if (loop_ != null) {
            loop_.offerTask(task);
        }
    }

    public boolean isInLoopThread() {
        return (loop_ != null) && loop_.isInLoopThread();
    }

    protected TransportListener getTransportListener() {
        return transportListener_;
    }

    @Override
    public void addListener(TransportListener listener) {
        Objects.requireNonNull(listener, "listener");

        synchronized (this) {
            TransportListener oldListener = transportListener_;
            if (oldListener == null) {
                transportListener_ = listener;
                return;
            }
            if (oldListener instanceof ListenerList) {
                ((ListenerList) oldListener).list_.add(listener);
                return;
            }
            ListenerList listenerList = new ListenerList();
            listenerList.list_.add(oldListener);
            listenerList.list_.add(listener);
            transportListener_ = listenerList;
        }
    }

    @Override
    public void removeListener(TransportListener listener) {
        Objects.requireNonNull(listener, "listener");

        synchronized (this) {
            if (transportListener_ == listener) {
                transportListener_ = NULL_LISTENER;
                return;
            }
            if (transportListener_ instanceof ListenerList) {
                ListenerList listenerList = (ListenerList) transportListener_;
                listenerList.list_.remove(transportListener_);
                if (listenerList.list_.size() == 1) {
                    transportListener_ = listenerList.list_.get(0);
                }
            }
        }
    }

    @Override
    public Object attach(Object attachment) {
        return attachmentReference_.getAndSet(attachment);
    }

    @Override
    public Object attachment() {
        return attachmentReference_.get();
    }

    abstract protected void writeDirect(BufferSink buffer);

    private static class NullListener implements TransportListener {
        @Override
        public void onBind(Transport transport, SocketAddress local) {
        }

        @Override
        public void onConnect(Transport transport, SocketAddress remote) {
        }

        @Override
        public void onJoin(Transport transport, InetAddress group, NetworkInterface ni, InetAddress source) {
        }

        @Override
        public void onClose(Transport transport) {
        }
    }

    private static class ListenerList implements TransportListener {

        CopyOnWriteArrayList<TransportListener> list_ = new CopyOnWriteArrayList<>();

        @Override
        public void onBind(Transport transport, SocketAddress local) {
            for (TransportListener listener : list_) {
                listener.onBind(transport, local);
            }
        }

        @Override
        public void onConnect(Transport transport, SocketAddress remote) {
            for (TransportListener listener : list_) {
                listener.onConnect(transport, remote);
            }
        }

        @Override
        public void onJoin(Transport transport, InetAddress group, NetworkInterface ni, InetAddress source) {
            for (TransportListener listener : list_) {
                listener.onJoin(transport, group, ni, source);
            }
        }

        @Override
        public void onClose(Transport transport) {
            for (TransportListener listener : list_) {
                listener.onClose(transport);
            }
        }
    }
}
