package net.ihiroky.niotty.nio;

/**
 * Created on 13/01/17, 18:11
 *
 * @author Hiroki Itoh
 */
public class ConnectSelectorPool extends AbstractSelectorPool<ConnectSelector> {
    @Override
    protected ConnectSelector newEventLoop() {
        return new ConnectSelector();
    }
}
