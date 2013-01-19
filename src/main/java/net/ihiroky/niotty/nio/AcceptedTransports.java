package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.Transport;

import java.nio.channels.SelectableChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created on 13/01/15, 15:47
 *
 * @author Hiroki Itoh
 */
public class AcceptedTransports {

    private ConcurrentMap<SelectableChannel, Transport> transportMap;

    AcceptedTransports() {
        transportMap = new ConcurrentHashMap<SelectableChannel, Transport>();
    }

    public Transport storeTransportIfAbsent(SelectableChannel channel, Transport transport) {
        Transport old = transportMap.putIfAbsent(channel, transport);
        return (old == null) ? transport : old;
    }

    public Transport getTransport(SelectableChannel channel) {
        return transportMap.get(channel);
    }
}
