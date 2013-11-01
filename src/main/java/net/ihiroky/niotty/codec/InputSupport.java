package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;

/**
 * @author Hiroki Itoh
 */
public class InputSupport {

    private CodecBuffer buffer_;

    /**
     * <p>Reads data to the amount of specified {@code requiredLength}.</p>
     *
     * <p>If enough data exist in the {@code input}, then this method returns a buffer
     * which contains data at least {@code requiredLength}. Otherwise, returns null
     * and pools a content of the {@code input} to a member field.</p>
     *
     * @param input a input buffer
     * @param requiredLength expected read length
     * @param noCopyIfEnough true if this method returns the {@code input} when the {@code input}
     *                       has enough data to read data to the amount of {@code requiredLength},
     *                       or returns new copied buffer.
     * @return the buffer which contains the data at least the {@code requiredLength}, or null.
     */
    CodecBuffer readFully(CodecBuffer input, int requiredLength, boolean noCopyIfEnough) {
        if (buffer_ != null) {
            buffer_.drainFrom(input, requiredLength - buffer_.remaining());
            if (buffer_.remaining() == requiredLength) {
                CodecBuffer fulfilled = buffer_;
                buffer_ = null;
                return fulfilled;
            }
            return null;
        }

        int remainingBytes = input.remaining();
        if (remainingBytes >= requiredLength) {
            return noCopyIfEnough ? input : copy(input, requiredLength);
        }
        if (remainingBytes == 0) {
            return null;
        }
        buffer_ = copy(input, requiredLength);
        return null;
    }

    private static CodecBuffer copy(CodecBuffer input, int bytes) {
        CodecBuffer b = Buffers.wrap(new byte[bytes], 0, 0);
        b.drainFrom(input, bytes);
        return b;
    }

    CodecBuffer getPooling() {
        return buffer_;
    }
}
