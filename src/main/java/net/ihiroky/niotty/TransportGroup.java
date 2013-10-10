package net.ihiroky.niotty;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class TransportGroup implements TransportFutureListener {

    private final ConcurrentMap<Transport, Boolean> transportMap_;
    private final boolean removeOnClose_;
    private final Set<Transport> transportViewSet_;

    public TransportGroup() {
        this(true);
    }

    public TransportGroup(boolean removeOnClose) {
        transportMap_ = new ConcurrentHashMap<Transport, Boolean>();
        removeOnClose_ = removeOnClose;
        transportViewSet_ = Collections.unmodifiableSet(transportMap_.keySet());
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
        if (transportMap_.putIfAbsent(transport, Boolean.FALSE) == null && removeOnClose_) {
            transport.closeFuture().addListener(this);
        }
     }

    public void remove(Transport transport) {
        transportMap_.remove(transport);
    }

    public Set<Transport> childSet() {
        return transportViewSet_;
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
