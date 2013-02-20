package net.ihiroky.niotty;

import java.util.Set;

/**
 * @author Hiroki Itoh
 */
public interface TransportAggregate {
    Set<Transport> childSet();
}
