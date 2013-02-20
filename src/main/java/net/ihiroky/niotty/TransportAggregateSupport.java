package net.ihiroky.niotty;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Hiroki Itoh
 */
public class TransportAggregateSupport implements TransportAggregate, TransportListener {

    private final ConcurrentMap<Transport, Boolean> transportMap;
    private final boolean removeOnClose;
    // TODO eviction by time if Transport is closed.

    public TransportAggregateSupport() {
        this(true);
    }

    public TransportAggregateSupport(boolean removeOnClose) {
        this.transportMap = new ConcurrentHashMap<>();
        this.removeOnClose = removeOnClose;
    }

    public void write(Object message) {
        for (Transport transport : transportMap.keySet()) {
            transport.write(message);
        }
    }

    public void close() {
        for (Transport transport : transportMap.keySet()) {
            transport.close();
        }
    }

    public void add(Transport transport) {
        if (!(transport instanceof AbstractTransport)) {
            throw new IllegalArgumentException("transport must be an instance of AbstractTransport.");
        }
        AbstractTransport<?> abstractTransport = (AbstractTransport<?>) transport;
        if (transportMap.putIfAbsent(abstractTransport, Boolean.FALSE) == null && removeOnClose) {
            transport.addListener(this);
        }
        // log
    }

    public void remove(Transport transport) {
        transportMap.remove(transport);
    }

    @Override
    public Set<Transport> childSet() {
        return transportMap.keySet();
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
