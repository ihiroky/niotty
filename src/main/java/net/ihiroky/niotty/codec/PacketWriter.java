package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.buffer.Packet;

/**
 * Provides the interface to write {@link net.ihiroky.niotty.buffer.Packet} into some device.
 */
public interface PacketWriter {

    /**
     * Writes the packet into some device.
     * @param packet the packet to be written
     */
    void write(Packet packet);

    /**
     * Rolls over the stored data.
     */
    void rollOver();

    /**
     * Closes related resources.
     */
    void close();
}
