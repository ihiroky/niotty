package net.ihiroky.niotty;

/**
 *
 */
public interface Stage {
    void stored(StageContext context, Object message);
    void loaded(StageContext context, Object message);
    void exceptionCaught(StageContext context, Exception exception);
    void activated(StageContext context);
    void deactivated(StageContext context, Pipeline.DeactivateState state);
}
