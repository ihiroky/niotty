package net.ihiroky.niotty;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class TransportAggregateSupport implements TransportAggregate, TransportFutureListener {

    private final ConcurrentMap<Transport, Boolean> transportMap_;
    private final boolean removeOnClose_;
    // TODO eviction by time if Transport is closed.

    public TransportAggregateSupport() {
        this(true);
    }

    public TransportAggregateSupport(boolean removeOnClose) {
        this.transportMap_ = new ConcurrentHashMap<>();
        this.removeOnClose_ = removeOnClose;
    }

    public void write(Object message) {
        for (Transport transport : transportMap_.keySet()) {
            transport.write(message);
        }
    }

    public void write(Object message, TransportParameter parameter) {
        for (Transport transport : transportMap_.keySet()) {
            transport.write(message, parameter);
        }
    }

    public void close() {
        for (Transport transport : transportMap_.keySet()) {
            transport.close();
        }
    }

    public void add(Transport transport) {
        if (!(transport instanceof AbstractTransport)) {
            throw new IllegalArgumentException("transport must be an instance of AbstractTransport.");
        }
        AbstractTransport<?> abstractTransport = (AbstractTransport<?>) transport;
        if (transportMap_.putIfAbsent(abstractTransport, Boolean.FALSE) == null && removeOnClose_) {
            transport.closeFuture().addListener(this);
        }
        // log
    }

    public void remove(Transport transport) {
        transportMap_.remove(transport);
    }

    @Override
    public Set<Transport> childSet() {
        return transportMap_.keySet();
    }

    @Override
    public void onComplete(TransportFuture future) {
        transportMap_.remove(future.transport());
    }

    @Override
    public String toString() {
        return transportMap_.toString();
    }
}
