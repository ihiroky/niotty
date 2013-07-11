package net.ihiroky.niotty.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * A configuration for {@code java.nio.channels.ServerSocketChannel} and its accepted
 * {@code java.nio.channels.SocketChannel}.
 *
 * @author Hiroki Itoh
 */
public class NioServerSocketConfig {

    private int backlog_;
    private Map<SocketOption<?>, Object> socketOptionMap_;
    private NioClientSocketConfig clientConfig_;

    private Logger logger_ = LoggerFactory.getLogger(NioServerSocketTransport.class);

    /**
     * Constructs a new instance.
     */
    public NioServerSocketConfig() {
        backlog_ = 50;
        socketOptionMap_ = new HashMap<>();
        clientConfig_ = new NioClientSocketConfig();
    }

    /**
     * Sets a socket option.
     *
     * @param name a name of the option
     * @param value a value of the option
     * @param <T> a type of the value
     * @return this config
     */
    public <T> NioServerSocketConfig setOption(SocketOption<T> name, Object value) {
        socketOptionMap_.put(name, value);
        return this;
    }

    /**
     * Returns a socket option.
     *
     * @param name a name of the option
     * @param <T> a type of the option's value
     * @return a value of the option
     */
    public <T> T option(SocketOption<T> name) {
        Object value = socketOptionMap_.get(name);
        return name.type().cast(value);
    }

    private <T> void logOptionValue(ServerSocketChannel channel, SocketOption<T> option) throws IOException {
        logger_.info("{}@{}'s {}: {}", channel, channel.hashCode(), option, channel.getOption(option));
    }

    void applySocketOptions(ServerSocketChannel channel) {
        try {
            for (Map.Entry<SocketOption<?>, Object> entry : socketOptionMap_.entrySet()) {
                @SuppressWarnings("unchecked")
                SocketOption<Object> name = (SocketOption<Object>) entry.getKey();
                channel.setOption(name, entry.getValue());
            }

            logOptionValue(channel, StandardSocketOptions.SO_RCVBUF);
            logOptionValue(channel, StandardSocketOptions.SO_REUSEADDR);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Sets a socket option of an accepted socket.
     *
     * @param name a name of the option
     * @param value a value of the option
     * @param <T> a type of the value
     * @return this config
     */
    public <T> NioServerSocketConfig setClientOption(SocketOption<T> name, Object value) {
        clientConfig_.setOption(name, value);
        return this;
    }

    /**
     * Returns a socket option of an accepted socket.
     * @param name a name of the option
     * @param <T> a type of the option's value
     * @return
     */
    public <T> T clientOption(SocketOption<T> name) {
        return clientConfig_.option(name);
    }

    void applySocketOptions(SocketChannel channel) {
        clientConfig_.applySocketOptions(channel);
    }

    /**
     * Sets the number of backlog of a server socket.
     * @param n the number of backlog
     * @return this config
     */
    public NioServerSocketConfig setBacklog(int n) {
        backlog_ = n;
        return this;
    }

    /**
     * Returns the number of backlog of a server socket.
     * @return  the number of backlog of a server socket
     */
    public int backlog() {
        return backlog_;
    }

    /**
     * Sets a write queue factory for an accepted socket.
     *
     * @param writeQueueFactory the write queue factory
     * @return this config
     */
    public NioServerSocketConfig setWriteQueueFactory(WriteQueueFactory writeQueueFactory) {
        clientConfig_.setWriteQueueFactory(writeQueueFactory);
        return this;
    }

    WriteQueue newWriteQueue() {
        return clientConfig_.newWriteQueue();
    }
}
