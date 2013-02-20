package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.Niotty;
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

    private int sendBufferSize;
    private int receiveBufferSize;
    private boolean keepAlive;
    private boolean reuseAddress;
    private int linger;
    private boolean tcpNoDelay;

    private Logger logger = LoggerFactory.getLogger(NioClientSocketConfig.class);

    public NioClientSocketConfig() {
        setPipeLineFactory(Niotty.newEmptyPipeLineFactory(NioClientSocketProcessor.DEFAULT_NAME));
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
            logger.info("{}'s {}: {}", channel, option, channel.getOption(option));
        } catch (IOException ioe) {
            throw new RuntimeException("failed to set socket option:" + option + " for:" + channel);
        }
    }

    void applySocketOptions(SocketChannel channel) {
        if (sendBufferSize > 0) {
            setOption(channel, StandardSocketOptions.SO_SNDBUF, sendBufferSize);
        }
        if (receiveBufferSize > 0) {
            setOption(channel, StandardSocketOptions.SO_RCVBUF, receiveBufferSize);
        }
        if (linger > 0) {
            setOption(channel, StandardSocketOptions.SO_LINGER, linger);
        }
        setOption(channel, StandardSocketOptions.SO_REUSEADDR, reuseAddress);
        setOption(channel, StandardSocketOptions.SO_KEEPALIVE, keepAlive);
        setOption(channel, StandardSocketOptions.TCP_NODELAY, tcpNoDelay);

        logOptionValue(channel, StandardSocketOptions.SO_SNDBUF);
        logOptionValue(channel, StandardSocketOptions.SO_RCVBUF);
        logOptionValue(channel, StandardSocketOptions.SO_LINGER);
        logOptionValue(channel, StandardSocketOptions.SO_REUSEADDR);
        logOptionValue(channel, StandardSocketOptions.SO_KEEPALIVE);
        logOptionValue(channel, StandardSocketOptions.TCP_NODELAY);
    }

    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public void setLinger(int linger) {
        this.linger = linger;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }
}
