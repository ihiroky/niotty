package net.ihiroky.niotty;

/**
 * Manages I/O threads and {@link Transport} instantiations.
 *
 * <h3>PipelineComposer life cycle</h3>
 * <p>The life cycle of {@link PipelineComposer} is managed by this instance.
 * If {@link #start()} is called, then {@link PipelineComposer#setUp()}
 * is called. And If {@link #stop()}, then {@link PipelineComposer#close()}.
 * The instance of {@code PipelineComposer} is set by {@link #setPipelineComposer(PipelineComposer)}.
 * It is used by the transport implementation to set up pipelines.</p>
 *
 * @param <T> the type of the transport to be created by this class.
 */
public interface Processor<T extends Transport> {

    /**
     * Starts I/O threads.
     */
    void start();

    /**
     * Stops I/O threads.
     */
    void stop();

    /**
     * Returns a name for this instance.
     * @return a name for this instance.
     */
    String name();

    /**
     * Sets a {@link PipelineComposer}.
     * @param composer a {@code PipelineComposer}.
     */
    Processor<T> setPipelineComposer(PipelineComposer composer);

    /**
     * Constructs the transport.
     * @return the transport.
     */
    T createTransport();
}
