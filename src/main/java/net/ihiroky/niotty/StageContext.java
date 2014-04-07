package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * Provides a {@link net.ihiroky.niotty.Stage} with the interface to access
 * its {@link net.ihiroky.niotty.Transport} which holds its {@link net.ihiroky.niotty.Pipeline}.
 * The stage can proceed the pipeline to the next stage via this object.
 */
public interface StageContext {
    /**
     * Returns the key to specify the stage in the pipeline.
     *
     * @return the key to specify the stage in the pipeline
     */
    StageKey key();

    /**
     * Returns the transport which holds the pipeline.
     *
     * @return the transport which holds the pipeline
     */
    Transport transport();

    /**
     * Proceeds the pipeline to the next stage.
     *
     * @param message a message to proceed
     * @param parameter a parameter to proceed
     */
    void proceed(Object message, Object parameter);

    /**
     * Schedules the event in the dispatcher. The dispatcher handles the stage
     * in the same thread.
     *
     * @param event the event
     * @param timeout the timeout to fire the event
     * @param timeUnit the unit of timeout
     * @return
     */
    EventFuture schedule(Event event, long timeout, TimeUnit timeUnit);

    /**
     * Returns true if the dispatcher of the next stage is different from the one
     * of this stage.
     *
     * @return true if the dispatcher of the next stage is different from
     * the one of this stage
     */
    boolean changesDispatcherOnProceed();
}
