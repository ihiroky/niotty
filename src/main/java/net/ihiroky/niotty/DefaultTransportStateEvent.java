package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public class DefaultTransportStateEvent extends TransportStateEvent {

    public DefaultTransportStateEvent(TransportState state, Object value) {
        super(state, value);
    }

    @Override
    public void execute() {
    }
}
