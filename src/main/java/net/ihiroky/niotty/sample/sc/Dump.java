package net.ihiroky.niotty.sample.sc;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TransportStateEvent;

/**
* @author Hiroki Itoh
*/
class Dump implements LoadStage<String, Void> {
    @Override
    public void load(StageContext<Void> context, String input) {
        System.out.println("remote: " + context.transport().remoteAddress() + " - " + input);
    }

    @Override
    public void load(StageContext<?> context, TransportStateEvent event) {
    }
}
