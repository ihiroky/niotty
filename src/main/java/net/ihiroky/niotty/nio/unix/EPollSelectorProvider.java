package net.ihiroky.niotty.nio.unix;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

/**
 *
 */
public class EPollSelectorProvider extends SelectorProvider {

    @Override
    public DatagramChannel openDatagramChannel() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pipe openPipe() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AbstractSelector openSelector() throws IOException {
        return null;
    }

    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketChannel openSocketChannel() throws IOException {
        throw new UnsupportedOperationException();
    }
}
