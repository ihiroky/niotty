package net.ihiroky.niotty;

/**
 *
 */
public abstract class LoadStage implements Stage {
    @Override
    public void stored(StageContext context, Object message) {
        context.proceed(message);
    }

    @Override
    public void activated(StageContext context) {
    }

    @Override
    public void deactivated(StageContext context, Pipeline.DeactivateState state) {
    }
}
