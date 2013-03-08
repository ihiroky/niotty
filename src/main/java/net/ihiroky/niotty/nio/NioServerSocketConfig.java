package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TransportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.util.Objects;

/**
 * Created on 13/01/15, 18:19
 *
 * @author Hiroki Itoh
 */
public class NioServerSocketConfig extends TransportConfig {

    private int backlog_;
    private int receiveBufferSize_;
    private boolean reuseAddress_;
    private WriteQueueFactory writeQueueFactory_;

    private Logger logger_ = LoggerFactory.getLogger(NioServerSocketTransport.class);

    public NioServerSocketConfig() {
        backlog_ = 50;
        reuseAddress_ = true;
        writeQueueFactory_ = new SimpleWriteQueueFactory();
    }

    private <T> void setOption(ServerSocketChannel channel, SocketOption<T> option, T value) {
        try {
            channel.setOption(option, value);
        } catch (IOException ioe) {
            throw new RuntimeException("failed to set socket option:" + option + " value:" + value + " for:" + channel);
        }
    }

    private <T> void logOptionValue(ServerSocketChannel channel, SocketOption<T> option) {
        try {
            logger_.info("{}'s {}: {}", channel, option, channel.getOption(option));
        } catch (IOException ioe) {
            throw new RuntimeException("failed to set socket option:" + option + " for:" + channel);
        }
    }

    void applySocketOptions(ServerSocketChannel channel) {
        Objects.requireNonNull(channel, "s");

        if (receiveBufferSize_ > 0) {
            setOption(channel, StandardSocketOptions.SO_RCVBUF, receiveBufferSize_);
        }
        setOption(channel, StandardSocketOptions.SO_REUSEADDR, reuseAddress_);

        logOptionValue(channel, StandardSocketOptions.SO_RCVBUF);
        logOptionValue(channel, StandardSocketOptions.SO_REUSEADDR);
    }

    public void setBacklog(int n) {
        backlog_ = n;
    }

    public int getBacklog() {
        return backlog_;
    }

    public void setReceiveBufferSize(int size) {
        receiveBufferSize_ = size;
    }

    public int getRecieveBufferSize() {
        return receiveBufferSize_;
    }

    public void setReuseAddress(boolean on) {
        reuseAddress_ = on;
    }

    public boolean getReuseAddress() {
        return reuseAddress_;
    }

    public void setWriteQueueFactory(WriteQueueFactory writeQueueFactory) {
        Objects.requireNonNull(writeQueueFactory, "writeQueueFactory");
        this.writeQueueFactory_ = writeQueueFactory;
    }

    public WriteQueue newWriteQueue() {
        return writeQueueFactory_.newriteQueue();
    }
}
