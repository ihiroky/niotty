package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * @author Hiroki Itoh
 */
public class DefaultTransportStateEvent extends TransportStateEvent {

    public DefaultTransportStateEvent(TransportState state, Object value) {
        super(state, value);
    }

    @Override
    public long execute(TimeUnit timeUnit) throws Exception {
        return TaskLoop.DONE;
    }
}
