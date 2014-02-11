package net.ihiroky.niotty.nio.unix;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;

/**
 *
 */
public class EPollSelectionKey extends AbstractSelectionKey {

    EPollSelectionKey(SelectableChannel channel, Selector selector, int ops) {

    }

    @Override
    public SelectableChannel channel() {
        return null;
    }

    @Override
    public Selector selector() {
        return null;
    }

    @Override
    public int interestOps() {
        return 0;
    }

    @Override
    public SelectionKey interestOps(int ops) {
        return null;
    }

    @Override
    public int readyOps() {
        return 0;
    }
}
