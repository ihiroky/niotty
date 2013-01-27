package net.ihiroky.niotty;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Hiroki Itoh
 */
public class DefaultTransportAggregate implements TransportAggregate, TransportListener {

    protected final ConcurrentMap<Transport, Boolean> transportMap = new ConcurrentHashMap<>();

    @Override
    public void write(Object message) {
        for (Transport transport : transportMap.keySet()) {
            transport.write(message);
        }
    }

    @Override
    public void close() {
        for (Transport transport : transportMap.keySet()) {
            transport.close();
        }
    }

    @Override
    public void add(Transport transport) {
        if (!(transport instanceof AbstractTransport)) {
            throw new IllegalArgumentException("transport must be an instance of AbstractTransport.");
        }
        AbstractTransport<?> abstractTransport = (AbstractTransport<?>) transport;
        if (transportMap.putIfAbsent(abstractTransport, Boolean.FALSE) == null) {
            transport.addListener(this);
        }
        // log
    }

    @Override
    public void remove(Transport transport) {
        transportMap.remove(transport);
    }

    @Override
    public void onOpen(Transport transport) {
    }

    @Override
    public void onBind(Transport transport, SocketAddress localAddress) {
    }

    @Override
    public void onConnect(Transport transport, SocketAddress remoteAddress) {
    }

    @Override
    public void onJoin(Transport transport, InetAddress group, NetworkInterface networkInterface, InetAddress source) {
    }

    @Override
    public void onClose(Transport transport) {
        transportMap.remove(transport);
    }
}
