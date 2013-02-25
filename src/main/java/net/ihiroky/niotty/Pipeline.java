package net.ihiroky.niotty;

/**
 * Created on 13/01/09, 17:21
 *
 * @author Hiroki Itoh
 */
public interface Pipeline {

    StageContext<Object, Object> getFirstContext();
    StageContext<Object, Object> getLastContext();
    StageContext<Object, Object> searchContextFor(Class<?> stageClass);
    <S> S searchStageFor(Class<S> stageClass);
}
