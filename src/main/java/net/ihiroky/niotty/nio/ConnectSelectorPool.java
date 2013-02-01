package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.StageContextListener;

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

    @Override
    protected StageContextListener<?> newStoreStageContextListener() {
        return AbstractSelector.SELECTOR_STORE_CONTEXT_LISTENER;
    }
}
