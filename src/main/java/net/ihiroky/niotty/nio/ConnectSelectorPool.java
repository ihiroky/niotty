package net.ihiroky.niotty.nio;

import java.util.Objects;

/**
 * Created on 13/01/17, 18:11
 *
 * @author Hiroki Itoh
 */
public class ConnectSelectorPool extends AbstractSelectorPool<ConnectSelector> {

    private final MessageIOSelectorPool messageIOSelectorPool_;

    public ConnectSelectorPool(MessageIOSelectorPool messageIOSelectorPool) {
        Objects.requireNonNull(messageIOSelectorPool, "messageIOSelectorPool");
        messageIOSelectorPool_ = messageIOSelectorPool;
    }

    @Override
    protected ConnectSelector newEventLoop() {
        return new ConnectSelector(messageIOSelectorPool_);
    }
}
