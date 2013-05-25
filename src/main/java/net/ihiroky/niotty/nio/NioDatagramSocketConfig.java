package net.ihiroky.niotty.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class NioDatagramSocketConfig {

    private Map<SocketOption<?>, Object> socketOptionMap_;
    private WriteQueueFactory writeQueueFactory_;

    private Logger logger_ = LoggerFactory.getLogger(NioDatagramSocketConfig.class);

    public NioDatagramSocketConfig() {
        socketOptionMap_ = new HashMap<>();
        writeQueueFactory_ = new SimpleWriteQueueFactory();

        setOption(StandardSocketOptions.SO_REUSEADDR, true);
    }

    @SuppressWarnings("unchecked")
    public <T> void setOption(SocketOption<T> option, T value) {
        socketOptionMap_.put(option, value);
    }

    public <T> T getOption(SocketOption<T> option) {
        Object value = socketOptionMap_.get(option);
        return option.type().cast(value);
    }

    private <T> void logOptionValue(DatagramChannel channel, SocketOption<T> option) throws IOException {
        logger_.info("{}'s {}: {}", channel, option, channel.getOption(option));
    }

    void applySocketOptions(DatagramChannel channel) {
        try {
            for (Map.Entry<SocketOption<?>, Object> entry : socketOptionMap_.entrySet()) {
                @SuppressWarnings("unchecked")
                SocketOption<Object> name = (SocketOption<Object>) entry.getKey();
                channel.setOption(name, entry.getValue());
            }

            logOptionValue(channel, StandardSocketOptions.SO_RCVBUF);
            logOptionValue(channel, StandardSocketOptions.SO_SNDBUF);
            logOptionValue(channel, StandardSocketOptions.SO_REUSEADDR);
            logOptionValue(channel, StandardSocketOptions.SO_BROADCAST);
            logOptionValue(channel, StandardSocketOptions.IP_TOS);
            logOptionValue(channel, StandardSocketOptions.IP_MULTICAST_IF);
            logOptionValue(channel, StandardSocketOptions.IP_MULTICAST_TTL);
            logOptionValue(channel, StandardSocketOptions.IP_MULTICAST_LOOP);
        } catch (IOException ioe) {
            throw new RuntimeException("failed to apply socket options.", ioe);
        }
    }

    WriteQueue newWriteQueue() {
        return writeQueueFactory_.newWriteQueue();
    }

    public void setWriteQueueFactory_(WriteQueueFactory writeQueueFactory) {
        Objects.requireNonNull(writeQueueFactory, "writeQueueFactory");
        this.writeQueueFactory_ = writeQueueFactory;
    }
}
