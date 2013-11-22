package net.ihiroky.niotty.sample.sc;

import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;

/**
 *
 */
class Dump extends LoadStage {

    private boolean echo_;

    public Dump() {
        echo_ = false;
    }

    public Dump(boolean echo) {
        echo_ = echo;
    }

    @Override
    public void loaded(StageContext context, Object message, Object parameter) {
        System.out.println("remote: " + context.transport().remoteAddress() + " - " + message);
        if (echo_) {
            context.transport().write(message);
        }
    }

    @Override
    public void exceptionCaught(StageContext context, Exception exception) {
        exception.printStackTrace();
    }

    @Override
    public void activated(StageContext context) {
    }

    @Override
    public void deactivated(StageContext context, DeactivateState state) {
        context.transport().close();
    }
}
