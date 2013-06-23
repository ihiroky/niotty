package net.ihiroky.niotty.nio;

import java.util.Objects;

/**
 * Created on 13/01/17, 18:11
 *
 * @author Hiroki Itoh
 */
public class ConnectSelectorPool extends AbstractSelectorPool<ConnectSelector> {

    private final TcpIOSelectorPool ioSelectorPool_;

    public ConnectSelectorPool(TcpIOSelectorPool ioSelectorPool) {
        Objects.requireNonNull(ioSelectorPool, "ioSelectorPool");
        ioSelectorPool_ = ioSelectorPool;
    }

    @Override
    protected ConnectSelector newTaskLoop() {
        return new ConnectSelector(ioSelectorPool_);
    }
}
