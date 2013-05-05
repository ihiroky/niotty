package net.ihiroky.niotty.buffer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Hiroki Itoh
 */
public enum EncodedLengthCalculator {
    ASCII(StandardCharsets.US_ASCII) {
        @Override
        public int calculateBytesLength(String s) {
            return s.length();
        }
    },
    UTF_8(StandardCharsets.UTF_8) {
        @Override
        public int calculateBytesLength(String s) {
            int length = s.length();
            int bytes = 0;
            char c;
            for (int i = 0; i < length; i++) {
                c = s.charAt(i);
                if (c < '\u0080') {
                    bytes++;
                } else if (c < '\u0800' || Character.isSurrogate(c)) {
                    bytes += 2;
                } else {
                    bytes += 3;
                }
            }
            return bytes;
        }
    };

    final Charset charset_;

    EncodedLengthCalculator(Charset charset) {
        charset_ = charset;
    }

    /**
     * Returns the length of encoded bytes.
     * @param s string to be encoded
     * @return the length of encoded bytes
     */
    public abstract int calculateBytesLength(String s);
}
