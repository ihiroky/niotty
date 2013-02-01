package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.StageContextListener;

/**
 * Created on 13/01/15, 17:10
 *
 * @author Hiroki Itoh
 */
public class AcceptSelectorPool extends AbstractSelectorPool<AcceptSelector> {

    AcceptSelectorPool() {
    }

    @Override
    protected AcceptSelector newEventLoop() {
        return new AcceptSelector();
    }

    @Override
    protected StageContextListener<?> newStoreStageContextListener() {
        return AbstractSelector.SELECTOR_STORE_CONTEXT_LISTENER;
    }
}
