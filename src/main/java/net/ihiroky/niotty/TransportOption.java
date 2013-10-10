package net.ihiroky.niotty;

/**
 *
 */
public interface TransportOption<T> {
    T cast(Object value);
}
