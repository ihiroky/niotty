package net.ihiroky.niotty;

import net.ihiroky.niotty.buffer.CodecBuffer;

import java.nio.ByteBuffer;

/**
 * <p>Provides a chain of {@link net.ihiroky.niotty.Stage}s to process
 * transmission data and states of a {@link net.ihiroky.niotty.Transport}
 * which has this pipeline.</p>
 *
 * <p>Each stage is associated with {@link net.ihiroky.niotty.StageKey}. The stage
 * key must be unique in a pipeline.</p>
 *
 * <p>The special stage key {@link #IO_STAGE_KEY} is reserved for Niotty to specify
 * I/O stage. An user must not use it, or throws {@code IllegalArgumentException}
 * by add remove and replace operation.</p>
 *
 * <p>Pipeline notifies messages to be stored and loaded, transport state changes
 * and exceptions which occurs in the I/O thread to stages in it. The stored messages
 * is notified from the head of the stages. The loaded messages, transport state changes
 * and exceptions are notified from the tail of the stages. The head and tail
 * is determined by the {@link #add(net.ihiroky.niotty.StageKey, net.ihiroky.niotty.Stage)}
 * and so on.</p>
 *
 * <h4>Thread model</h4>
 * <p>Each stage is executed in the {@link EventDispatcher} to which
 * the transport belongs by default. Use {@link DefaultEventDispatcherGroup}
 * to allocate dedicated threads for a stage when it is added to the pipeline.
 * The {@code DefaultEventDispatcherGroup} must be shutdown in application shutdown procedure.
 * See {@link net.ihiroky.niotty.PipelineComposer} to synchronize its lifecycle with Niotty.</p>
 */
public interface Pipeline {

    /** The stage key to specify I/O stage. */
    StageKey IO_STAGE_KEY = StageKeys.of("IO_STAGE_KEY");

    /**
     * Adds the specified key and stage to the end of this pipeline.
     *
     * @param key the stage key which is associated with the stage
     * @param stage the stage to be added
     * @return this pipeline
     * @throws IllegalArgumentException the key is {@link #IO_STAGE_KEY} or already exist in this pipeline
     * @throws NullPointerException if the key or stage is null
     */
    Pipeline add(StageKey key, Stage stage);

    /**
     * Adds the specified key and stage to the end of this pipeline.
     *
     * @param key the stage key which is associated with the stage
     * @param stage the stage to be added
     * @param pool the pool which offers the EventDispatcher to execute the stage
     * @throws IllegalArgumentException the key is {@link #IO_STAGE_KEY} or already exist in this pipeline
     * @throws NullPointerException if the key or stage is null
     */
    Pipeline add(StageKey key, Stage stage, EventDispatcherGroup<? extends EventDispatcher> pool);

    /**
     * Adds the specified key and stage before the stage specified with the base stage key.
     *
     * @param baseKey the base stage key
     * @param key the stage key which is associated with the stage
     * @param stage the stage to be added
     * @return this pipeline
     * @throws IllegalArgumentException the key is {@link #IO_STAGE_KEY} or already exist in this pipeline
     * @throws NullPointerException if the baseKey, key or stage is null
     * @throws java.util.NoSuchElementException if the base stage key is not found in this pipeline
     */
    Pipeline addBefore(StageKey baseKey, StageKey key, Stage stage);

    /**
     * Adds the specified key and stage before the stage specified with the base stage key.
     *
     * @param baseKey the base stage key
     * @param key the stage key which is associated with the stage
     * @param stage the stage to be added
     * @param pool the pool which offers the EventDispatcher to execute the stage
     * @return this pipeline
     * @throws IllegalArgumentException the key is {@link #IO_STAGE_KEY} or already exist in this pipeline
     * @throws NullPointerException if the baseKey, key or stage is null
     * @throws java.util.NoSuchElementException if the base stage key is not found in this pipeline
     */
    Pipeline addBefore(StageKey baseKey, StageKey key, Stage stage, EventDispatcherGroup<? extends EventDispatcher> pool);

    /**
     * Adds the specified key and stage after the stage specified with the base stage key.
     *
     * @param baseKey the base stage key
     * @param key the stage key which is associated with the stage
     * @param stage the stage to be added
     * @return this pipeline
     * @throws IllegalArgumentException the key is {@link #IO_STAGE_KEY} or already exist in this pipeline
     * @throws NullPointerException if the baseKey, key or stage is null
     * @throws java.util.NoSuchElementException if the base stage key is not found in this pipeline
     */
    Pipeline addAfter(StageKey baseKey, StageKey key, Stage stage);

    /**
     * Adds the specified key and stage after the stage specified with the base stage key.
     *
     * @param baseKey the base stage key
     * @param key the stage key which is associated with the stage
     * @param stage the stage to be added
     * @param pool the pool which offers the EventDispatcher to execute the stage
     * @return this pipeline
     * @throws IllegalArgumentException the key is {@link #IO_STAGE_KEY} or already exist in this pipeline
     * @throws NullPointerException if the baseKey, key or stage is null
     * @throws java.util.NoSuchElementException if the base stage key is not found in this pipeline
     */
    Pipeline addAfter(StageKey baseKey, StageKey key, Stage stage, EventDispatcherGroup<? extends EventDispatcher> pool);

    /**
     * Removes a stage specified with the stage key.
     *
     * @param key the stage key which is associated with the stage to be removed
     * @return this pipeline
     * @throws IllegalArgumentException the key is {@link #IO_STAGE_KEY} or already exist in this pipeline
     * @throws NullPointerException if the key is null
     * @throws java.util.NoSuchElementException if the base stage key is not found in this pipeline
     */
    Pipeline remove(StageKey key);

    /**
     * Replaces a stage associated with the old key to the new stage associated with the new key.
     *
     * @param oldKey the old key which specifies the stage to be removed
     * @param newKey the new key which is associated with the new stage
     * @param newStage the new stage to be added
     * @return this pipeline
     * @throws IllegalArgumentException the new key is {@link #IO_STAGE_KEY} or already exist in this pipeline
     * @throws NullPointerException if the old key, new key, new stage is null
     * @throws java.util.NoSuchElementException if the stage associated with the old key key is not found
     *         in this pipeline
     */
    Pipeline replace(StageKey oldKey, StageKey newKey, Stage newStage);

    /**
     * Replaces a stage associated with the old key to the new stage associated with the new key.
     *
     * @param oldKey the old key which specifies the stage to be removed
     * @param newKey the new key which is associated with the new stage
     * @param newStage the new stage to be added
     * @param pool the pool which offers the EventDispatcher to execute the stage
     * @return this pipeline
     * @throws IllegalArgumentException the new key is {@link #IO_STAGE_KEY} or already exist in this pipeline
     * @throws NullPointerException if the old key, new key, new stage is null
     * @throws java.util.NoSuchElementException if the stage associated with the old key key is not found
     *         in this pipeline
     */
    Pipeline replace(StageKey oldKey, StageKey newKey, Stage newStage, EventDispatcherGroup<? extends EventDispatcher> pool);

    /**
     * Calls a message store chain in this pipeline.
     * @param message the message
     */
    void store(Object message);


    /**
     * Calls a message store chain with a parameter in this pipeline.
     * @param message the message
     * @param parameter the parameter
     */
    void store(Object message, Object parameter);

    /**
     * Calls a message load chain in this pipeline.
     * @param message the message
     */
    void load(ByteBuffer message);

    /**
     * Calls a message load chain with a parameter in this pipeline.
     * @param message the message
     * @param parameter the parameter
     */
    void load(ByteBuffer message, Object parameter);

    /**
     * Calls a message load chain with a parameter in this pipeline.
     * @param message the message
     * @param parameter the parameter
     */
    void load(CodecBuffer message, Object parameter);

    /**
     * Calls {@link net.ihiroky.niotty.Stage#activated(net.ihiroky.niotty.StageContext)}s in this pipeline.
     */
    void activate();

    /**
     * Calls {@link net.ihiroky.niotty.Stage#deactivated(net.ihiroky.niotty.StageContext)}s in this pipeline.
     */
    void deactivate();

    /**
     * Calls {@link net.ihiroky.niotty.Stage#exceptionCaught(net.ihiroky.niotty.StageContext, java.lang.Exception)}s
     * in this pipeline.
     * @param exception the exception thrown in the I/O thread
     */
    void catchException(Exception exception);

    /**
     * Calls {@link net.ihiroky.niotty.Stage#eventTriggered(net.ihiroky.niotty.StageContext, java.lang.Object)}s
     * in this pipeline.
     * @param event the event triggered in the I/O thread
     */
    void eventTriggered(Object event);

    /**
     * Returns the transport to which this pipeline belongs to.
     * @return the transport to which this pipeline belongs to
     */
    Transport transport();

    /**
     * Returns the name of this pipeline.
     * @return the name of this pipeline
     */
    String name();

    /**
     * Searches a element that holds a stage specified with the {@code key}.
     * @param key the key to specify the context
     * @return the context
     */
    PipelineElement searchElement(StageKey key);

}
