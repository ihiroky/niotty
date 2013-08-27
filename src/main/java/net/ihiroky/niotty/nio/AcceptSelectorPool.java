package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTaskTimer;

/**
 * Created on 13/01/15, 17:10
 *
 * @author Hiroki Itoh
 */
public class AcceptSelectorPool extends AbstractSelectorPool<AcceptSelector> {

    AcceptSelectorPool() {
    }

    @Override
    protected AcceptSelector newTaskLoop() {
        return new AcceptSelector(DefaultTaskTimer.NULL);
    }
}
