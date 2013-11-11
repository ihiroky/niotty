package net.ihiroky.niotty;

/**
 *
 */
public abstract class StoreStage implements Stage {
    @Override
    public void loaded(StageContext context, Object message) {
        context.proceed(message);
    }

    @Override
    public void activated(StageContext context) {
    }

    @Override
    public void deactivated(StageContext context, Pipeline.DeactivateState state) {
    }
}
