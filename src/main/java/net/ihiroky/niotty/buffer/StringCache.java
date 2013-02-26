package net.ihiroky.niotty.buffer;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Empty and one byte ascii string cache.
 * @author Hiroki Itoh
 */
final class StringCache {

    static final String EMPTY = "";
    private static final String[] ASCII = createAsciiCache();
    private static final Set<Charset> CHARSET_INCLUDE_ASCII = charsetContainingAscii();

    static final int MAX_ASCII = 0x7F;

    private StringCache() {
        throw new AssertionError();
    }

    private static String[] createAsciiCache() {
        String[] c = new String[MAX_ASCII + 1];
        for (int i = 0; i <= MAX_ASCII; i++) {
            c[i] = Character.toString((char) i);
        }
        return c;
    }

    private static Set<Charset> charsetContainingAscii() {
        Set<Charset> set = new HashSet<>();
        set.add(StandardCharsets.US_ASCII);
        set.add(StandardCharsets.ISO_8859_1);
        set.add(StandardCharsets.UTF_8);
        return set;
    }

    static boolean writeAsOneCharAscii(EncodeBuffer b, CharsetEncoder encoder, String s) {
        if (CHARSET_INCLUDE_ASCII.contains(encoder.charset())){
            char c =  s.charAt(0);
            if (c  <= StringCache.MAX_ASCII) {
                b.writeByte(c);
                return true;
            }
        }
        return false;
    }

    static String getCachedValue(DecodeBuffer b, CharsetDecoder decoder, int bytes) {
        switch (bytes) {
            case 0:
                return StringCache.EMPTY;
            case 1:
                return (CHARSET_INCLUDE_ASCII.contains(decoder.charset())) ? ASCII[b.readByte()] : null;
            default:
                return null;
        }
    }

    static String toString(CharBuffer output, CharsetDecoder decoder) {
        return (output.remaining() == 1 && CHARSET_INCLUDE_ASCII.contains(decoder.charset()))
                ? ASCII[output.get()] : output.toString();
    }
}
