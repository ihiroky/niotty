package net.ihiroky.niotty;

/**
 * <p>A element of {@link net.ihiroky.niotty.Pipeline} which encodes output messages and
 * decodes input messages, receives a transport state changes and exception which occurs
 * in I/O threads.</p>
 *
 * <p>{@link #stored(net.ihiroky.niotty.StageContext, Object, Object)} or
 * {@link #loaded(net.ihiroky.niotty.StageContext, Object, Object)} form a call chain,
 * this is controlled by each {@code Stage}. The first stage is triggered
 * by {@link net.ihiroky.niotty.Pipeline}, and if {@link net.ihiroky.niotty.StageContext#proceed(Object, Object)}
 * is called in these methods, the call chain is proceeded; {@code stored()} and {@code loaded()}
 * in the next stage is called. If not proceeded, the methods of the next stage is not called.
 * In contrast, all of {@link #activated(net.ihiroky.niotty.StageContext)},
 * {@link #deactivated(net.ihiroky.niotty.StageContext)} or
 * {@link #exceptionCaught(net.ihiroky.niotty.StageContext, Exception)} are called by the pipeline.
 * There is no need to call {@link net.ihiroky.niotty.StageContext#proceed(Object, Object)} for these methods.</p>
 *
 * <p>Each method of this class receives {@link net.ihiroky.niotty.StageContext}. The context
 * holds the related information to this stage, {@link net.ihiroky.niotty.StageKey}
 * and {@link net.ihiroky.niotty.Transport} etc.</p>
 */
public interface Stage {

    /**
     * Invoked when a message is stored to this stage.
     * @param context the context
     * @param message the message
     * @param parameter a parameter for the message
     */
    void stored(StageContext context, Object message, Object parameter);

    /**
     * Invoked when a message is loaded to this stage.
     * @param context the context
     * @param message the message
     * @param parameter a parameter for the message
     */
    void loaded(StageContext context, Object message, Object parameter);

    /**
     * Invoked when a exception occurs in the I/O threads.
     * @param context the context
     * @param exception the exception
     */
    void exceptionCaught(StageContext context, Exception exception);

    /**
     * Invoked when the transport gets readable, which associated to the pipeline to which this stage belongs.
     * @param context the context
     */
    void activated(StageContext context);

    /**
     * Invoked when the transport is closed, which associated to the pipeline to which this stage belongs.
     * @param context the context
     */
    void deactivated(StageContext context);

    /**
     * Invoked when an event which is not defined in this interface is triggered
     * in the transport which associated to the pipeline to which this stage belongs.
     *
     * @param event the event
     */
    void eventTriggered(StageContext context, Object event);
}
