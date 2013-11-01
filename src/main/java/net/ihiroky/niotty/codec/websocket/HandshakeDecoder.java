package net.ihiroky.niotty.codec.websocket;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.util.Charsets;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * @author Hiroki Itoh
 */
public class HandshakeDecoder implements LoadStage<CodecBuffer, Void> {

    private CodecBuffer buffer_;
    private RequestLine requestLine_;
    private HeaderFields headerFields_;
    private CharsetDecoder decoder_ = CHARSET.newDecoder();

    private static final byte[] DELIMITER = new byte[]{'\r', '\n'};
    private static final int INITIAL_STRING_BUFFER_LENGTH = 64;
    private static final Charset CHARSET = Charsets.US_ASCII;

    public void load(StageContext<Void> context, CodecBuffer input) {
        int end;
        String line = null;
        CodecBuffer b = bufferOfInput(input);
        while (b.remaining() > 0) {
            for (;;) {
                end = b.indexOf(DELIMITER, 0);
                if (end == -1) {
                    if (buffer_ != null) {
                        if (buffer_.remaining() > 0) {
                            buffer_.compact();
                        } else {
                            buffer_ = null;
                        }
                        return;
                    }
                    int remaining = input.remaining();
                    if (remaining > 0) {
                        buffer_ = Buffers.newCodecBuffer(remaining);
                        buffer_.drainFrom(input);
                    }
                    input.dispose();
                    return;
                }
                if (end > 0) {
                    line = b.readString(decoder_, end);
                    b.skipStartIndex(DELIMITER.length);
                    break;
                }
                break;
            }
            if (line == null) {
                sendHandshakeResponse(context.transport(), null, null);
                return;
            }
            if (requestLine_ == null) {
                requestLine_ = new RequestLine(line);
            }
            // parse header
        }
    }

    @Override
    public void load(StageContext<Void> context, TransportStateEvent event) {
    }

    private CodecBuffer bufferOfInput(CodecBuffer input) {
        if (buffer_ == null) {
            return input;
        }
        buffer_.drainFrom(input);
        input.dispose();
        return buffer_;
    }

    private void sendHandshakeResponse(Transport transport, RequestLine requestLine, HeaderFields headerFields) {

    }

    CodecBuffer buffer() {
        return buffer_;
    }
}
