package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
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

    private final MessageIOSelectorPool messageIOSelectorPool_;
    private Logger logger_ = LoggerFactory.getLogger(ConnectSelector.class);

    ConnectSelector(MessageIOSelectorPool messageIOSelectorPool) {
        messageIOSelectorPool_ = messageIOSelectorPool;
    }

    @Override
    protected void processSelectedKeys(Set<SelectionKey> selectedKeys) throws Exception {
        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext();) {
            SelectionKey key = i.next();
            i.remove();

            SocketChannel channel = (SocketChannel) key.channel();
            @SuppressWarnings("unchecked")
            TransportFutureAttachment<ConnectSelector> attachment =
                    (TransportFutureAttachment<ConnectSelector>) key.attachment();
            try {
                if (channel.finishConnect()) {
                    logger_.info("new channel {} is connected.", channel);
                    unregister(key);

                    ConnectionWaitTransport cwt = (ConnectionWaitTransport) attachment.getTransport();
                    registerReadLater(channel, cwt.transport(), attachment.getFuture());
                }
            } catch (IOException ioe) {
                attachment.getFuture().setThrowable(ioe);
            }
        }
    }

    void registerReadLater(SelectableChannel channel,
            NioClientSocketTransport transport, DefaultTransportFuture future) throws IOException {
        InetSocketAddress remoteAddress = transport.remoteAddress();
        future.done();
        messageIOSelectorPool_.register(channel, SelectionKey.OP_READ, transport);
        transport.fireOnConnect();
        transport.loadEventLater(new TransportStateEvent(TransportState.CONNECTED, remoteAddress));
    }
}
