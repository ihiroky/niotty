package net.ihiroky.niotty;

import java.util.Objects;
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

    private final DefaultLoadPipeline<T> loadPipeline_;
    private final DefaultStorePipeline<T> storePipeline_;
    private final AtomicReference<Object> attachmentReference_;
    private final DefaultTransportFuture closeFuture_;
    private final T loop_;

    /**
     * Creates a new instance.
     *
     * @param name a name of this transport
     * @param pipelineComposer a composer to initialize a pipeline for this transport
     * @param taskLoopGroup the pool which offers the TaskLoop to execute the stage
     */
    protected AbstractTransport(
            String name, PipelineComposer pipelineComposer, TaskLoopGroup<T> taskLoopGroup) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(pipelineComposer, "pipelineComposer");

        attachmentReference_ = new AtomicReference<Object>();
        closeFuture_ = new DefaultTransportFuture(this);
        loop_ = taskLoopGroup.assign(this);

        DefaultLoadPipeline<T> loadPipeline = new DefaultLoadPipeline<T>(name, this, taskLoopGroup);
        DefaultStorePipeline<T> storePipeline = new DefaultStorePipeline<T>(name, this, taskLoopGroup);
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

    @Override
    public LoadPipeline loadPipeline() {
        return loadPipeline_;
    }

    @Override
    public StorePipeline storePipeline() {
        return storePipeline_;
    }

    /**
     * Closes pipelines.
     */
    public void closePipelines() {
        loadPipeline_.close();
        storePipeline_.close();
    }

    /**
     * Sets the specified stage at the end of the store pipeline.
     * @param ioStage the stage
     */
    public void setIOStage(StoreStage<?, ?> ioStage) {
        Objects.requireNonNull(ioStage, "ioStage");
        storePipeline_.setTailStage(ioStage);
    }

    /**
     * Gets the instance of {@link net.ihiroky.niotty.TaskLoop}.
     * @return <T> the TaskLoop.
     */
    public T taskLoop() {
        return loop_;
    }

    @Override
    public DefaultTransportFuture closeFuture() {
        return closeFuture_;
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
    public Object attach(Object attachment) {
        return attachmentReference_.getAndSet(attachment);
    }

    @Override
    public Object attachment() {
        return attachmentReference_.get();
    }
}
