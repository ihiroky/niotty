package net.ihiroky.niotty.codec;

import com.jcraft.jzlib.GZIPException;
import com.jcraft.jzlib.Inflater;
import com.jcraft.jzlib.JZlib;
import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TransportException;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;

import java.util.zip.DataFormatException;

/**
 * @author Hiroki Itoh
 */
public class JZlibInflaterDecoder extends LoadStage {

    private Inflater inflater_;
    private byte[] buffer_;
    private byte[] dictionary_;
    private byte[] output_;
    private boolean deactivated_;

    protected static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int MAX_OUTPUT_BUFFER_SIZE = 8192;
    private static final int OUTPUT_SCALE = 4;
    private static final int INITIAL_SCALED_SIZE_LIMIT = MAX_OUTPUT_BUFFER_SIZE / OUTPUT_SCALE;
    private static final int MIN_OUTPUT_BUFFER_SIZE = 128;

    public JZlibInflaterDecoder() {
        this(DEFAULT_BUFFER_SIZE, null, false);
    }

    public JZlibInflaterDecoder(boolean gzip) {
        this(DEFAULT_BUFFER_SIZE, null, gzip);
    }

    public JZlibInflaterDecoder(int bufferSize, byte[] dictionary, boolean gzip) {
        try {
            inflater_ = new Inflater(gzip ? JZlib.WrapperType.GZIP : JZlib.WrapperType.ZLIB);
        } catch (GZIPException e) {
            throw new TransportException("Failed to create Inflater.", e);
        }
        buffer_ = new byte[bufferSize];
        dictionary_ = dictionary;
    }

    private static int outputBufferSize(int remaining) {
        return (remaining <= INITIAL_SCALED_SIZE_LIMIT) ? remaining * OUTPUT_SCALE : MAX_OUTPUT_BUFFER_SIZE;
    }

    private static int outputBufferSize(int remaining, int decompressed, int processed) {
        int next = (processed != 0)
                ? (int) (((float) remaining / processed) * decompressed) // processed / remaining = consumption rate
                : decompressed * OUTPUT_SCALE;
        return (next <= MAX_OUTPUT_BUFFER_SIZE) ? next : MIN_OUTPUT_BUFFER_SIZE;
    }

    @Override
    public void loaded(StageContext context, Object message, Object parameter) {
        CodecBuffer input = (CodecBuffer) message;
        byte[] buffer;
        int offset;
        int length;
        while (input.remaining() > 0) {
            if (input.hasArray()) {
                buffer = input.array();
                offset = input.arrayOffset() + input.startIndex();
                length = input.remaining();
                input.skipStartIndex(length);
            } else {
                buffer = buffer_;
                offset = 0;
                length = input.readBytes(buffer, 0, buffer.length);
            }
            try {
                if (inflate(context, buffer, offset, length, parameter)) {
                    // input.skipStartIndex(inflater_.getRemaining());
                    input.skipStartIndex(inflater_.next_in_index);
                    break;
                }
            } catch (DataFormatException dfe) {
                throw new RuntimeException("Data format error.", dfe);
            }
        }
        input.dispose();
    }

    private boolean inflate(StageContext context,  byte[] buffer, int offset, int length, Object parameter)
            throws DataFormatException {

        Inflater inflater = inflater_;
        byte[] output = (output_ == null) ? new byte[outputBufferSize(length)] : output_;
        inflater.setInput(buffer, offset, length, false);
        for (;;) {
            inflater.setOutput(output, 0, output.length);
            int nextOutIndex = inflater.next_out_index;
            int status = inflater.inflate(JZlib.Z_SYNC_FLUSH);
            if (status == JZlib.Z_OK
                    || status == JZlib.Z_BUF_ERROR
                    || status == JZlib.Z_STREAM_END) {
                int decompressed = inflater.next_out_index - nextOutIndex;
                if (decompressed > 0) {
                    CodecBuffer b = Buffers.wrap(output, 0, decompressed);
                    context.proceed(b, parameter);
                    int newRemaining = inflater.avail_in;
                    if (newRemaining <= 0) {
                        output_ = null;
                        return status == JZlib.Z_STREAM_END;
                    }
                    int processed = length - newRemaining;
                    output = new byte[outputBufferSize(length, decompressed, processed)];
                    length = newRemaining;
                    continue;
                }
                output_ = null;
                return status == JZlib.Z_STREAM_END;
            } else if (status == JZlib.Z_NEED_DICT) {
                if (dictionary_ == null) {
                    throw new RuntimeException("A dictionary is required.");
                }
                status = inflater.setDictionary(dictionary_, dictionary_.length);
                JZlibDeflaterEncoder.checkStatus(status, "Failed to set dictionary");
                continue;
            }
            JZlibDeflaterEncoder.checkStatus(status, "Failed to inflate data");
        }
    }

    @Override
    public void exceptionCaught(StageContext context, Exception exception) {
    }

    @Override
    public void activated(StageContext context) {
    }

    @Override
    public void deactivated(StageContext context, DeactivateState state) {
        if (!deactivated_
                && (state == DeactivateState.LOAD || state == DeactivateState.WHOLE)) {
            inflater_.end();
            output_ = null;
        }
    }

    byte[] output() {
        return output_;
    }
}
