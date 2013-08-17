package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TaskTimer;
import net.ihiroky.niotty.buffer.BufferSink;
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
public class AcceptSelector extends AbstractSelector {

    private Logger logger_ = LoggerFactory.getLogger(AcceptSelector.class);

    /**
     * Creates a new instance.
     *
     * @param timer the timer to execute the tasks.
     * @throws NullPointerException if timer is null.
     */
    protected AcceptSelector(TaskTimer timer) {
        super(timer);
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
