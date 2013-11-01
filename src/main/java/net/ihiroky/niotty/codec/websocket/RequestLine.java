package net.ihiroky.niotty.codec.websocket;

/**
 * @author Hiroki Itoh
 */
public class RequestLine {

    private String method_;
    private String uri_;
    private String version_;

    private static final char SEPARATOR = ' ';

    public RequestLine(String method, String uri, String version) {
        method_ = method;
        uri_ = uri;
        version_ = version;
    }

    public RequestLine(String requestLine) {
        int from = 0;
        int to = requestLine.indexOf(SEPARATOR, from);
        if (to == -1) {
            throw new IllegalArgumentException("Illegal format: [" + requestLine + "]");
        }
        method_ = requestLine.substring(from, to);
        from = to + 1;
        to = requestLine.indexOf(SEPARATOR, from);
        if (to == -1 || to == requestLine.length()) {
            throw new IllegalArgumentException("Illegal format: [" + requestLine + "]");
        }
        uri_ = requestLine.substring(from, to);
        version_= requestLine.substring(to + 1);
    }

    private static int indexOf(byte[] b, byte value, int from) {
        int length = b.length;
        for (int i = from; i < length; i++) {
            if (b[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public String method() {
        return method_;
    }

    public String uri() {
        return uri_;
    }

    public String version() {
        return version_;
    }
}
