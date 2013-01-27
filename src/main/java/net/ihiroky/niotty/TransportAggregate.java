package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface TransportAggregate {

    void write(Object message);
    void close();
    void add(Transport transport);
    void remove(Transport transport);
}
