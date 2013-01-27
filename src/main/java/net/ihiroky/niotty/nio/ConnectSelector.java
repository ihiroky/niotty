package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.event.TransportState;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created on 13/01/17, 18:10
 *
 * @author Hiroki Itoh
 */
public class ConnectSelector extends AbstractSelector<ConnectSelector> {

    private Logger logger = LoggerFactory.getLogger(ConnectSelector.class);

    @Override
    protected void processSelectedKeys(Set<SelectionKey> selectedKeys) throws Exception {
        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
            SelectionKey key = i.next();
            i.remove();

            SocketChannel channel = (SocketChannel) key.channel();
            if (channel.finishConnect()) {
                logger.info("new channel {} is connected.", channel);
                channel.configureBlocking(false);

                NioClientSocketTransport parent = (NioClientSocketTransport) key.attachment();
                key.interestOps(0);

                NioChildChannelTransport child = parent.registerLater(channel, SelectionKey.OP_READ);
                child.loadEventLater(
                        new TransportStateEvent(child, TransportState.CONNECTED, channel.getRemoteAddress()));
            }
        }
    }
}
