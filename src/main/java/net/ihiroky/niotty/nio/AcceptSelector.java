package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.buffer.BufferSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * An implementation of {@link net.ihiroky.niotty.nio.AbstractSelector} to handle asynchronous acceptances.
 */
public class AcceptSelector extends AbstractSelector {

    private Logger logger_ = LoggerFactory.getLogger(AcceptSelector.class);

    /**
     * Creates a new instance.
     */
    protected AcceptSelector() {
    }

    @Override
    protected void processSelectedKeys(Set<SelectionKey> selectedKeys) throws Exception {
        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext();) {
            SelectionKey key = i.next();
            i.remove();

            NioServerSocketTransport transport = (NioServerSocketTransport) key.attachment();
            ServerSocketChannel parentChannel = (ServerSocketChannel) key.channel();
            SocketChannel childChannel = parentChannel.accept();
            logger_.info("new channel {} is accepted.", childChannel);
            childChannel.configureBlocking(false);

            transport.registerReadLater(childChannel);
        }
    }

    @Override
    public void store(StageContext<Void> context, BufferSink input) {
    }
}
