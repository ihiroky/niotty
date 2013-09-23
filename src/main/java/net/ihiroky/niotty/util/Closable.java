package net.ihiroky.niotty.util;

/**
 * A resource that must be closed when it is no longer needed.
 */
public interface Closable {
    void close();
}
