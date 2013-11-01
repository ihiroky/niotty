package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * @author Hiroki Itoh
 */
public interface BufferSource {
    // input : channel, StageContext, used in StoreStage -> InputStage
    // current IOStage -> OutputStage
    void transferFrom(ReadableByteChannel channel, ByteBuffer readBuffer);
}
