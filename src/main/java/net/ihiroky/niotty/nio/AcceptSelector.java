package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
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

    private Logger logger_ = LoggerFactory.getLogger(AcceptSelector.class);

    @Override
    protected void processSelectedKeys(Set<SelectionKey> selectedKeys) throws Exception {
        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
            SelectionKey key = i.next();
            i.remove();

            ServerSocketChannel parentChannel = (ServerSocketChannel) key.channel();
            SocketChannel childChannel = parentChannel.accept();
            logger_.info("new channel {} is accepted.", childChannel);
            childChannel.configureBlocking(false);

            @SuppressWarnings("unchecked")
            TransportFutureAttachment<AcceptSelector> attachment =
                    (TransportFutureAttachment<AcceptSelector>) key.attachment();
            NioServerSocketTransport parent = (NioServerSocketTransport) attachment.getTransport();
            DefaultTransportFuture future = attachment.getFuture();
            parent.registerLater(childChannel, SelectionKey.OP_READ, future);
        }
    }
}
