package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Arguments;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>A skeletal implementation of {@code Transport}.</p>
 *
 * <p>This class holds a load (inbound) and store (outbound) pipeline, an attachment reference and an event listener.
 * {@link EventDispatcher} is also held by this class, which handles asynchronous I/O operations</p>
 *
 * @param <T> The type of {@link EventDispatcher}
 */
public abstract class AbstractTransport<T extends EventDispatcher> implements Transport {

    private final DefaultPipeline<T> pipeline_;
    private final AtomicReference<Object> attachmentReference_;
    private final DefaultTransportFuture closeFuture_;
    private final T dispatcher_;

    /**
     * Creates a new instance.
     *
     * @param name a name of this transport
     * @param pipelineComposer a composer to initialize a pipeline for this transport
     * @param eventDispatcherGroup the pool which offers the EventDispatcher to execute the stage
     */
    protected AbstractTransport(
            String name, PipelineComposer pipelineComposer, EventDispatcherGroup<T> eventDispatcherGroup) {
        Arguments.requireNonNull(name, "name");
        Arguments.requireNonNull(pipelineComposer, "pipelineComposer");

        attachmentReference_ = new AtomicReference<Object>();
        closeFuture_ = new DefaultTransportFuture(this);
        dispatcher_ = eventDispatcherGroup.assign(this);

        DefaultPipeline<T> pipeline =
                new DefaultPipeline<T>(name, this, eventDispatcherGroup, Pipeline.IO_STAGE_KEY, ioStage());
        pipelineComposer.compose(pipeline);
        pipeline_ = pipeline;
    }

    /**
     * Creates a stage which handle I/O operation.
     * This method is called in the constructor, so the implementation class should define
     * this method as final and safe.
     *
     * @return the stage
     */
    protected abstract Stage ioStage();

    @Override
    public Pipeline pipeline() {
        return pipeline_;
    }

    /**
     * Closes pipelines.
     */
    public void closePipeline() {
        pipeline_.close();
    }

    /**
     * Gets the instance of {@link EventDispatcher}.
     * @return <T> the EventDispatcher.
     */
    public T eventDispatcher() {
        return dispatcher_;
    }

    @Override
    public DefaultTransportFuture closeFuture() {
        return closeFuture_;
    }

    @Override
    public void write(Object message) {
        pipeline_.store(message);
    }

    @Override
    public void write(Object message, Object parameter) {
        pipeline_.store(message, parameter);
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
