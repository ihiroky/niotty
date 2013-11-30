package net.ihiroky.niotty.codec;

import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.GZIPException;
import com.jcraft.jzlib.JZlib;
import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.TransportException;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;

import java.io.IOException;

/**
 * @author Hiroki Itoh
 */
public class JZlibDeflaterEncoder extends StoreStage {

    private final Deflater deflater_;
    private final BufferChannel buffer_;

    public static final int BEST_SPEED = JZlib.Z_BEST_SPEED;
    public static final int STANDARD_COMPRESSION = JZlib.Z_DEFAULT_COMPRESSION;
    public static final int BEST_COMPRESSION = JZlib.Z_BEST_COMPRESSION;

    protected static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int DEF_MEM_LEVEL = 8;
    private static final byte[] EMPTY = new byte[0];

    public JZlibDeflaterEncoder() {
        this(JZlib.Z_BEST_SPEED, DEFAULT_BUFFER_SIZE, null, false);
    }

    public JZlibDeflaterEncoder(int level) {
        this(level, DEFAULT_BUFFER_SIZE, null, false);
    }

    public JZlibDeflaterEncoder(int level, boolean gzip) {
        this(level, DEFAULT_BUFFER_SIZE, null, gzip);
    }

    public JZlibDeflaterEncoder(int level, int bufferSize, byte[] dictionary, boolean gzip) {
        try {
            deflater_ = new Deflater(level, JZlib.MAX_WBITS, DEF_MEM_LEVEL,
                    gzip ? JZlib.WrapperType.GZIP : JZlib.WrapperType.ZLIB);
        } catch (GZIPException e) {
            throw new TransportException("Failed to create Deflater.", e);
        }
        buffer_ = new BufferChannel(new byte[bufferSize]);
        if (dictionary != null) {
            int status = deflater_.setDictionary(dictionary, dictionary.length);
            checkStatus(status, "Failed to set dictionary");
        }
    }

    static void checkStatus(int status, String message) {
        if (status == JZlib.Z_OK) {
            return;
        }
        switch (status) {
            case JZlib.Z_STREAM_END:
                throw new TransportException(message + " (Z_STREAM_END).");
            case JZlib.Z_NEED_DICT:
                throw new TransportException(message + " (Z_NEED_DICT).");
            case JZlib.Z_ERRNO:
                throw new TransportException(message + " (Z_ERRNO).");
            case JZlib.Z_STREAM_ERROR:
                throw new TransportException(message + " (Z_STREAM_ERROR).");
            case JZlib.Z_DATA_ERROR:
                throw new TransportException(message + " (Z_DATA_ERROR).");
            case JZlib.Z_MEM_ERROR:
                throw new TransportException(message + " (Z_MEM_ERROR).");
            case JZlib.Z_BUF_ERROR:
                throw new TransportException(message + " (Z_BUF_ERROR).");
            case JZlib.Z_VERSION_ERROR:
                throw new TransportException(message + " (Z_VERSION_ERROR).");
            default:
                throw new UnsupportedOperationException("Unsupported status:" + status);
        }
    }

    @Override
    public void stored(StageContext context, Object message, Object parameter) {
        BufferSink input = (BufferSink) message;
        CodecBuffer output = input.hasArray() ? compressRawArray(input) : compressBufferSink(input);
        input.dispose();
        context.proceed(output, parameter);
    }

    @Override
    public void exceptionCaught(StageContext context, Exception exception) {
    }

    @Override
    public void activated(StageContext context) {
    }

    private CodecBuffer compressRawArray(BufferSink input) {
        CodecBuffer output = Buffers.newCodecBuffer((int) (input.remaining() * 0.7f + 10));

        byte[] inputBytes = input.array();
        int inputOffset = input.arrayOffset();
        int inputLength = input.remaining();

        deflater_.setInput(inputBytes, inputOffset, inputLength, false);
        byte[] outputBytes = output.array();
        int outputOffset = output.endIndex();
        int outputLength = outputBytes.length;
        for (;;) {
            deflater_.setOutput(outputBytes, outputOffset, outputLength - outputOffset);
            int nextOutIndex = deflater_.next_out_index;
            int status = deflater_.deflate(JZlib.Z_SYNC_FLUSH);
            checkStatus(status, "Failed to compress data");
            outputOffset += deflater_.next_out_index - nextOutIndex;
            if (deflater_.avail_in <= 0) {
                output.endIndex(outputOffset);
                return output;
            }
            if (outputOffset == outputLength) {
                CodecBuffer newOutput = Buffers.newCodecBuffer(outputLength);
                newOutput.writeBytes(outputBytes, 0, outputOffset);
                outputBytes = newOutput.array();
                outputLength = outputBytes.length;
            }
        }
    }

    private CodecBuffer compressBufferSink(BufferSink input) {
        CodecBuffer output = Buffers.newCodecBuffer((int) (input.remaining() * 0.7f + 10));

        try {
            while (input.remaining() > 0) {
                buffer_.reset();
                input.sink(buffer_);
                byte[] inputBytes = buffer_.array();
                int inputLength = buffer_.position();

                deflater_.setInput(inputBytes, 0, inputLength, false);
                byte[] outputBytes = output.array();
                int outputOffset = output.endIndex();
                int outputLength = outputBytes.length;
                for (;;) {
                    deflater_.setOutput(outputBytes, outputOffset, outputLength - outputOffset);
                    int nextOutputIndex = deflater_.next_out_index;
                    int status = deflater_.deflate(JZlib.Z_SYNC_FLUSH);
                    checkStatus(status, "Failed to compress data");
                    outputOffset += deflater_.next_in_index - nextOutputIndex;
                    if (deflater_.avail_in <= 0) {
                        output.endIndex(outputOffset);
                        return output;
                    }
                    if (outputOffset == outputLength) {
                        CodecBuffer newOutput = Buffers.newCodecBuffer(outputLength);
                        newOutput.writeBytes(outputBytes, 0, outputOffset);
                        outputBytes = newOutput.array();
                        outputLength = outputBytes.length;
                    }
                }
            }
            return output;
        } catch (IOException ignored) {
            throw new AssertionError(ignored);
        }
    }

    @Override
    public void deactivated(StageContext context, DeactivateState state) {
        if (!deflater_.finished()
                && (state == DeactivateState.STORE || state == DeactivateState.WHOLE)) {
            final int bufferLength = 16;
            CodecBuffer output = Buffers.newCodecBuffer(bufferLength);
            byte[] buffer = new byte[bufferLength];
            deflater_.setInput(EMPTY);
            while (!deflater_.finished()) {
                deflater_.setOutput(buffer, 0, buffer.length);
                int nextOutputIndex = deflater_.next_out_index;
                int status = deflater_.deflate(JZlib.Z_FINISH);
                if (status != JZlib.Z_OK && status != JZlib.Z_STREAM_END) {
                    throw new TransportException("Failed to finish compression (" + status + ").");
                }
                int n = deflater_.next_out_index - nextOutputIndex;
                output.writeBytes(buffer, 0, n);
            }
            deflater_.end();
            if (output.remaining() > 0) {
                context.proceed(output, null); // may be error on non-connected udp
            }
        }
    }
}
