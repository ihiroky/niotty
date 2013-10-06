package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Arguments;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>A skeletal implementation of {@code Transport}.</p>
 *
 * <p>This class holds a load (inbound) and store (outbound) pipeline, an attachment reference and an event listener.
 * {@link net.ihiroky.niotty.TaskLoop} is also held by this class, which handles asynchronous I/O operations</p>
 *
 * @param <T> The type of {@link net.ihiroky.niotty.TaskLoop}
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
        Arguments.requireNonNull(name, "name");
        Arguments.requireNonNull(pipelineComposer, "pipelineComposer");

        attachmentReference_ = new AtomicReference<Object>();
        closeFuture_ = new DefaultTransportFuture(this);
        loop_ = taskLoopGroup.assign(this);

        DefaultLoadPipeline<T> loadPipeline = new DefaultLoadPipeline<T>(name, this, taskLoopGroup);
        DefaultStorePipeline<T> storePipeline = new DefaultStorePipeline<T>(name, this, taskLoopGroup, ioStage());
        pipelineComposer.compose(loadPipeline, storePipeline);

        loadPipeline_ = loadPipeline;
        storePipeline_ = storePipeline;
    }

    /**
     * Creates a stage which handle I/O operation.
     * The implementation class should define this method as final and safe.
     * This method is called in the constructor.
     * @return the stage
     */
    protected abstract StoreStage<?, ?> ioStage();

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
        storePipeline_.execute(message);
    }

    @Override
    public void write(Object message, TransportParameter parameter) {
        storePipeline_.execute(message, parameter);
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
        storePipeline_.execute(message, new DefaultTransportParameter(priority));
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
