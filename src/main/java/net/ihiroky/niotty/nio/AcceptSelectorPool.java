package net.ihiroky.niotty.nio;

/**
 * Created on 13/01/15, 17:10
 *
 * @author Hiroki Itoh
 */
public class AcceptSelectorPool extends AbstractSelectorPool<AcceptSelector> {

    AcceptSelectorPool() {
    }

    @Override
    protected AcceptSelector createEventLoop() {
        return new AcceptSelector();
    }
}
