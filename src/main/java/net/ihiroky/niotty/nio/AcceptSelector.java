package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.event.TransportState;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created on 13/01/15, 16:34
 *
 * @author Hiroki Itoh
 */
public class AcceptSelector extends AbstractSelector<AcceptSelector> {

    private Logger logger = LoggerFactory.getLogger(AcceptSelector.class);

    @Override
    protected void processSelectedKeys(Set<SelectionKey> selectedKeys) throws Exception {
        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
            SelectionKey key = i.next();
            i.remove();

            ServerSocketChannel parentChannel = (ServerSocketChannel) key.channel();
            SocketChannel childChannel = parentChannel.accept();
            logger.info("new channel {} is accepted.", childChannel);
            childChannel.configureBlocking(false);

            NioServerSocketTransport parent = (NioServerSocketTransport) key.attachment();
            NioChildChannelTransport child = parent.registerLater(childChannel, SelectionKey.OP_READ);
            child.loadEventLater(
                    new TransportStateEvent(child, TransportState.ACCEPTED, childChannel.getRemoteAddress()));
        }
    }
}
