package net.ihiroky.niotty;

import net.ihiroky.niotty.buffer.BufferSink;

import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>A skeletal implementation of {@code Transport}.</p>
 *
 * <p>This class holds a load (inbound) and store (outbound) pipeline, an attachment reference and an event listener.
 * {@link net.ihiroky.niotty.TaskLoop} is also held by this class, which handles asynchronous I/O operations</p>
 *
 * @param <T> The type of {@link net.ihiroky.niotty.TaskLoop}
 * @author Hiroki Itoh
 */
public abstract class AbstractTransport<T extends TaskLoop> implements Transport, TaskSelection {

    private volatile DefaultLoadPipeline loadPipeline_;
    private volatile DefaultStorePipeline storePipeline_;
    private final AtomicReference<Object> attachmentReference_;
    private TransportListener transportListener_;
    private T loop_;
    private final int weight_;

    private static final TransportListener NULL_LISTENER = new NullListener();

    /**
     * Creates a new instance.
     */
    protected AbstractTransport(String name, PipelineComposer pipelineComposer, int weight) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(pipelineComposer, "pipelineComposer");
        if (weight <= 0) {
            throw new IllegalArgumentException("The weight must be positive.");
        }

        attachmentReference_ = new AtomicReference<>();
        transportListener_ = NULL_LISTENER;
        weight_ = weight;
        setUpPipelines(name, pipelineComposer);
    }

    /**
     * <p>Initializes the load / store pipeline with a specified pipeline composer.</p>
     *
     * @param baseName a name used in {@link net.ihiroky.niotty.AbstractPipeline}.
     * @param pipelineComposer the composer to set up the load / store pipeline.
     */
    private void setUpPipelines(String baseName, PipelineComposer pipelineComposer) {

        DefaultLoadPipeline loadPipeline = new DefaultLoadPipeline(baseName, this);
        DefaultStorePipeline storePipeline = new DefaultStorePipeline(baseName, this);
        pipelineComposer.compose(loadPipeline, storePipeline);

        loadPipeline.verifyStageType();
        storePipeline.verifyStageType();

        loadPipeline_ = loadPipeline;
        storePipeline_ = storePipeline;
    }

    /**
     * <p>Executes the load pipeline.</p>
     * @param message A message to be processed.
     */
    protected void executeLoad(Object message) {
        loadPipeline_.execute(message);
    }

    /**
     * <p>Executes the load pipeline.</p>
     * @param message A message to be processed.
     * @param parameter A parameter which is passed to I/O implementation.
     */
    protected void executeLoad(Object message, TransportParameter parameter) {
        loadPipeline_.execute(message, parameter);
    }

    /**
     * <p>Executes the load pipeline.</p>
     * @param stateEvent an event to change transport state.
     */
    protected void executeLoad(TransportStateEvent stateEvent) {
        loadPipeline_.execute(stateEvent);
    }

    /**
     * <p>Executes the store pipeline.</p>
     * @param message A message to be processed.
     */
    protected void executeStore(Object message) {
        storePipeline_.execute(message);
    }

    /**
     * <p>Executes the store pipeline.</p>
     * @param message A message to be processed.
     * @param parameter A parameter which is passed to I/O implementation.
     */
    protected void executeStore(Object message, TransportParameter parameter) {
        storePipeline_.execute(message, parameter);
    }

    /**
     * <p>Executes the store pipeline.</p>
     * @param stateEvent an event to change transport state.
     */
    protected void executeStore(TransportStateEvent stateEvent) {
        storePipeline_.execute(stateEvent);
    }

    /**
     * Resets the pipelines with the specified composer.
     * @param composer the composer to reset pipelines.
     */
    public final void resetPipelines(PipelineComposer composer) {
        Objects.requireNonNull(composer, "composer");

        // use the same lock object as listener to save memory footprint.
        synchronized (this) {
            DefaultLoadPipeline oldLoadPipeline = loadPipeline_;
            DefaultStorePipeline oldStorePipeline = storePipeline_;
            DefaultLoadPipeline loadPipelineCopy = oldLoadPipeline.createCopy();
            DefaultStorePipeline storePipelineCopy = oldStorePipeline.createCopy();
            composer.compose(loadPipelineCopy, storePipelineCopy);

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

    /**
     * Closes pipelines.
     */
    public final void closePipelines() {
        loadPipeline_.close();
        storePipeline_.close();
    }

    /**
     * Adds the specified stage at the end of the store pipeline.
     * @param ioStage the stage
     */
    public final void addIOStage(StoreStage<BufferSink, Void> ioStage) {
        if (storePipeline_ == null) {
            throw new IllegalStateException("setUpPipelines() is not called.");
        }
        Objects.requireNonNull(ioStage, "ioStage");
        storePipeline_.addIOStage(ioStage);
    }

    @Override
    public int weight() {
        return weight_;
    }

    /**
     * Sets the instance of {@link net.ihiroky.niotty.TaskLoop}.
     * @param loop the the instance of {@code TaskLoop}.
     */
    public final void setTaskLoop(T loop) {
        Objects.requireNonNull(loop, "loop");
        this.loop_ = loop;
    }

    /**
     * Gets the instance of {@link net.ihiroky.niotty.TaskLoop}.
     * @return <T> the TaskLoop.
     */
    protected final T taskLoop() {
        return loop_;
    }

    /**
     * Returns the listener.
     * @return the listener.
     */
    protected TransportListener transportListener() {
        return transportListener_;
    }

    @Override
    public void write(Object message) {
        executeStore(message);
    }

    @Override
    public void write(Object message, TransportParameter parameter) {
        executeStore(message, parameter);
    }

    /**
     * <p>Writes a specified message to this transport with a specified priority.</p>
     *
     * <p>An invocation of this method behaves in exactly the same way as the invocation
     * {@code write(message, new DefaultTransportParameter(priority))}.</p>
     *
     * @param message the message
     * @param priority the priority
     */
    public void write(Object message, int priority) {
        executeStore(message, new DefaultTransportParameter(priority));
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

    /**
     * A null object for TransportListener.
     */
    private static class NullListener implements TransportListener {
        @Override
        public void onAccept(Transport transport, SocketAddress remote) {
        }
        @Override
        public void onConnect(Transport transport, SocketAddress remote) {
        }
        @Override
        public void onClose(Transport transport) {
        }
    }

    /**
     * An aggregator for TransportListener.
     */
    private static class ListenerList implements TransportListener {

        CopyOnWriteArrayList<TransportListener> list_ = new CopyOnWriteArrayList<>();

        @Override
        public void onAccept(Transport transport, SocketAddress remote) {
            for (TransportListener listener : list_) {
                listener.onAccept(transport, remote);
            }
        }

        @Override
        public void onConnect(Transport transport, SocketAddress remote) {
            for (TransportListener listener : list_) {
                listener.onConnect(transport, remote);
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
