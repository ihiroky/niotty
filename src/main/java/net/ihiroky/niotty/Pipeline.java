package net.ihiroky.niotty;

/**
 * Created on 13/01/09, 17:21
 * TODO preapre context iterator for rebuild pipeline
 * @author Hiroki Itoh
 */
public interface Pipeline<S> {

    Pipeline<S> add(StageKey key, S stage);
    Pipeline<S> add(StageKey key, S stage, StageContextExecutorPool pool);
    Pipeline<S> addBefore(StageKey baseKey, StageKey key, S stage);
    Pipeline<S> addBefore(StageKey baseKey, StageKey key, S stage, StageContextExecutorPool pool);
    Pipeline<S> addAfter(StageKey baseKey, StageKey key, S stage);
    Pipeline<S> addAfter(StageKey baseKey, StageKey key, S stage, StageContextExecutorPool pool);
    Pipeline<S> remove(StageKey key);
    Pipeline<S> replace(StageKey oldKey, StageKey newKey, S newStage);
    Pipeline<S> replace(StageKey oldKey, StageKey newKey, S newStage, StageContextExecutorPool pool);
    String name();
    Transport transport();
}
