package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TaskTimer;

/**
 * Created on 13/01/17, 18:11
 *
 * @author Hiroki Itoh
 */
public class ConnectSelectorPool extends AbstractSelectorPool<ConnectSelector> {

    @Override
    protected ConnectSelector newTaskLoop() {
        return new ConnectSelector(TaskTimer.NULL);
    }
}
