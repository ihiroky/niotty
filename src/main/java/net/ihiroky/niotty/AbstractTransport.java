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
public abstract class AbstractTransport implements Transport {

    private final AtomicReference<Object> attachmentReference_;
    private final DefaultTransportFuture closeFuture_;
    private final EventDispatcher dispatcher_;

    /**
     * Creates a new instance.
     *
     * @param name a name of this transport
     * @param pipelineComposer a composer to initialize a pipeline for this transport
     * @param eventDispatcherGroup the pool which offers the EventDispatcher to execute the stage
     */
    protected AbstractTransport(
            String name, PipelineComposer pipelineComposer, EventDispatcherGroup eventDispatcherGroup) {
        Arguments.requireNonNull(name, "name");
        Arguments.requireNonNull(pipelineComposer, "pipelineComposer");

        attachmentReference_ = new AtomicReference<Object>();
        closeFuture_ = new DefaultTransportFuture(this);
        dispatcher_ = eventDispatcherGroup.assign(this);
    }

    /**
     * Gets the instance of the {@link net.ihiroky.niotty.EventDispatcher},
     * which handles the I/O requests.
     *
     * @return the EventDispatcher.
     */
    public EventDispatcher eventDispatcher() {
        return dispatcher_;
    }

    @Override
    public DefaultTransportFuture closeFuture() {
        return closeFuture_;
    }

    @Override
    public void write(Object message) {
        pipeline().store(message);
    }

    @Override
    public void write(Object message, Object parameter) {
        pipeline().store(message, parameter);
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
