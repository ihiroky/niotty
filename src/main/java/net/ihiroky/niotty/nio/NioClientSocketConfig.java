package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TransportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;

/**
 * Created on 13/01/17, 18:01
 *
 * @author Hiroki Itoh
 */
public class NioClientSocketConfig extends TransportConfig {

    private int sendBufferSize_;
    private int receiveBufferSize_;
    private boolean keepAlive_;
    private boolean reuseAddress_;
    private int linger_;
    private boolean tcpNoDelay_;

    private Logger logger_ = LoggerFactory.getLogger(NioClientSocketConfig.class);

    public NioClientSocketConfig() {
    }

    private <T> void setOption(SocketChannel channel, SocketOption<T> option, T value) {
        try {
            channel.setOption(option, value);
        } catch (IOException ioe) {
            throw new RuntimeException("failed to set socket option:" + option + " value:" + value + " for:" + channel);
        }
    }

    private <T> void logOptionValue(SocketChannel channel, SocketOption<T> option) {
        try {
            logger_.info("{}'s {}: {}", channel, option, channel.getOption(option));
        } catch (IOException ioe) {
            throw new RuntimeException("failed to set socket option:" + option + " for:" + channel);
        }
    }

    void applySocketOptions(SocketChannel channel) {
        if (sendBufferSize_ > 0) {
            setOption(channel, StandardSocketOptions.SO_SNDBUF, sendBufferSize_);
        }
        if (receiveBufferSize_ > 0) {
            setOption(channel, StandardSocketOptions.SO_RCVBUF, receiveBufferSize_);
        }
        if (linger_ > 0) {
            setOption(channel, StandardSocketOptions.SO_LINGER, linger_);
        }
        setOption(channel, StandardSocketOptions.SO_REUSEADDR, reuseAddress_);
        setOption(channel, StandardSocketOptions.SO_KEEPALIVE, keepAlive_);
        setOption(channel, StandardSocketOptions.TCP_NODELAY, tcpNoDelay_);

        logOptionValue(channel, StandardSocketOptions.SO_SNDBUF);
        logOptionValue(channel, StandardSocketOptions.SO_RCVBUF);
        logOptionValue(channel, StandardSocketOptions.SO_LINGER);
        logOptionValue(channel, StandardSocketOptions.SO_REUSEADDR);
        logOptionValue(channel, StandardSocketOptions.SO_KEEPALIVE);
        logOptionValue(channel, StandardSocketOptions.TCP_NODELAY);
    }

    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize_ = sendBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize_ = receiveBufferSize;
    }

    public void setKeepAlive(boolean keepAlive_) {
        this.keepAlive_ = keepAlive_;
    }

    public void setReuseAddress(boolean reuseAddress_) {
        this.reuseAddress_ = reuseAddress_;
    }

    public void setLinger(int linger_) {
        this.linger_ = linger_;
    }

    public void setTcpNoDelay(boolean tcpNoDelay_) {
        this.tcpNoDelay_ = tcpNoDelay_;
    }
}
