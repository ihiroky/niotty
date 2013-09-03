package net.ihiroky.niotty;

/**
 * <p>Provides a set of {@link LoadStage} or {@link StoreStage} to process
 * transmission data and states of a {@link Transport} which has this pipeline.</p>
 *
 * <p>Each stage is associated with {@link StageKey}. The stage key must be unique
 * in a pipeline.</p>
 *
 * <p>The special stage key {@link #IO_STAGE} is reserved for Niotty to specify
 * I/O stage. An user must not use it, or throws {@code IllegalArgumentException}
 * by add remove and replace operation.</p>
 *
 * <h4>Thread model</h4>
 * <p>Each stage is executed in the {@link TaskLoop} to which the transport belongs
 * by default. Use {@link DefaultTaskLoopGroup} to allocate dedicated threads for
 * a stage when it is added to the pipeline. The {@code DefaultTaskLoopGroup} must
 * be shutdown in application shutdown procedure. See {@link PipelineComposer}
 * to synchronize its lifecycle with Niotty.</p>
 *
 * @param <S> the type of the stage
 */
public interface Pipeline<S> {

    /** The stage key to specify I/O stage. */
    StageKey IO_STAGE = StageKeys.of("IO_STAGE");

    /**
     * Adds the specified key and stage to the end of this pipeline.
     *
     * @param key the stage key which is associated with the stage
     * @param stage the stage to be added
     * @return this pipeline
     * @throws IllegalArgumentException the key is {@link #IO_STAGE} or already exist in this pipeline
     * @throws NullPointerException if the key or stage is null
     */
    Pipeline<S> add(StageKey key, S stage);

    /**
     * Adds the specified key and stage to the end of this pipeline.
     *
     * @param key the stage key which is associated with the stage
     * @param stage the stage to be added
     * @param pool the pool which offers the TaskLoop to execute the stage
     * @throws IllegalArgumentException the key is {@link #IO_STAGE} or already exist in this pipeline
     * @throws NullPointerException if the key or stage is null
     */
    Pipeline<S> add(StageKey key, S stage, PipelineElementExecutorPool pool);

    /**
     * Adds the specified key and stage before the stage specified with the base stage key.
     *
     * @param baseKey the base stage key
     * @param key the stage key which is associated with the stage
     * @param stage the stage to be added
     * @return this pipeline
     * @throws IllegalArgumentException the key is {@link #IO_STAGE} or already exist in this pipeline
     * @throws NullPointerException if the baseKey, key or stage is null
     * @throws java.util.NoSuchElementException if the base stage key is not found in this pipeline
     */
    Pipeline<S> addBefore(StageKey baseKey, StageKey key, S stage);

    /**
     * Adds the specified key and stage before the stage specified with the base stage key.
     *
     * @param baseKey the base stage key
     * @param key the stage key which is associated with the stage
     * @param stage the stage to be added
     * @param pool the pool which offers the TaskLoop to execute the stage
     * @return this pipeline
     * @throws IllegalArgumentException the key is {@link #IO_STAGE} or already exist in this pipeline
     * @throws NullPointerException if the baseKey, key or stage is null
     * @throws java.util.NoSuchElementException if the base stage key is not found in this pipeline
     */
    Pipeline<S> addBefore(StageKey baseKey, StageKey key, S stage, PipelineElementExecutorPool pool);

    /**
     * Adds the specified key and stage after the stage specified with the base stage key.
     *
     * @param baseKey the base stage key
     * @param key the stage key which is associated with the stage
     * @param stage the stage to be added
     * @return this pipeline
     * @throws IllegalArgumentException the key is {@link #IO_STAGE} or already exist in this pipeline
     * @throws NullPointerException if the baseKey, key or stage is null
     * @throws java.util.NoSuchElementException if the base stage key is not found in this pipeline
     */
    Pipeline<S> addAfter(StageKey baseKey, StageKey key, S stage);

    /**
     * Adds the specified key and stage after the stage specified with the base stage key.
     *
     * @param baseKey the base stage key
     * @param key the stage key which is associated with the stage
     * @param stage the stage to be added
     * @param pool the pool which offers the TaskLoop to execute the stage
     * @return this pipeline
     * @throws IllegalArgumentException the key is {@link #IO_STAGE} or already exist in this pipeline
     * @throws NullPointerException if the baseKey, key or stage is null
     * @throws java.util.NoSuchElementException if the base stage key is not found in this pipeline
     */
    Pipeline<S> addAfter(StageKey baseKey, StageKey key, S stage, PipelineElementExecutorPool pool);

    /**
     * Removes a stage specified with the stage key.
     *
     * @param key the stage key which is associated with the stage to be removed
     * @return this pipeline
     * @throws IllegalArgumentException the key is {@link #IO_STAGE} or already exist in this pipeline
     * @throws NullPointerException if the key is null
     * @throws java.util.NoSuchElementException if the base stage key is not found in this pipeline
     */
    Pipeline<S> remove(StageKey key);

    /**
     * Replaces a stage associated with the old key to the new stage associated with the new key.
     *
     * @param oldKey the old key which specifies the stage to be removed
     * @param newKey the new key which is associated with the new stage
     * @param newStage the new stage to be added
     * @return this pipeline
     * @throws IllegalArgumentException the new key is {@link #IO_STAGE} or already exist in this pipeline
     * @throws NullPointerException if the old key, new key, new stage is null
     * @throws java.util.NoSuchElementException if the stage associated with the old key key is not found
     *         in this pipeline
     */
    Pipeline<S> replace(StageKey oldKey, StageKey newKey, S newStage);

    /**
     * Replaces a stage associated with the old key to the new stage associated with the new key.
     *
     * @param oldKey the old key which specifies the stage to be removed
     * @param newKey the new key which is associated with the new stage
     * @param newStage the new stage to be added
     * @param pool the pool which offers the TaskLoop to execute the stage
     * @return this pipeline
     * @throws IllegalArgumentException the new key is {@link #IO_STAGE} or already exist in this pipeline
     * @throws NullPointerException if the old key, new key, new stage is null
     * @throws java.util.NoSuchElementException if the stage associated with the old key key is not found
     *         in this pipeline
     */
    Pipeline<S> replace(StageKey oldKey, StageKey newKey, S newStage, PipelineElementExecutorPool pool);

    /**
     * Returns the name of this pipeline.
     * @return the name of this pipeline
     */
    String name();
}
