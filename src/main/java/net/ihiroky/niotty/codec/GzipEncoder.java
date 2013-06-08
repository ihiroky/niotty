package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.buffer.CodecBuffer;

import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * @author Hiroki Itoh
 */
public class GzipEncoder extends DeflaterEncoder {

    private final CRC32 crc32_;
    private boolean first_;
    private boolean finished_;

    private static final byte[] GZIP_HEADER = new byte[]{
            0x1f, (byte) 0x8b, Deflater.DEFLATED, 0, 0, 0, 0, 0, 0, 0
    };

    public GzipEncoder() {
        this(Deflater.BEST_SPEED, DeflaterEncoder.DEFAULT_BUFFER_SIZE);
    }

    public GzipEncoder(int level) {
        this(level, DeflaterEncoder.DEFAULT_BUFFER_SIZE);
    }

    public GzipEncoder(int level, int bufferSize) {
        super(level, bufferSize, null, true);
        crc32_ = new CRC32();
        first_ = true;
        finished_ = false;
    }

    @Override
    protected void onBeforeEncode(byte[] input, CodecBuffer output) {
        if (first_) {
            output.writeBytes(GZIP_HEADER, 0, GZIP_HEADER.length);
            first_ = false;
        }
        crc32_.update(input);
    }

    @Override
    protected void onAfterFinished(CodecBuffer output, Deflater deflater) {
        if (!finished_) {
            // write crc and totalIn with little endian
            int crc = (int) crc32_.getValue();
            output.writeByte(crc);
            output.writeByte(crc >>> 8);
            output.writeByte(crc >>> 16);
            output.writeByte(crc >>> 24);
            int totalIn = deflater.getTotalIn();
            output.writeByte(totalIn);
            output.writeByte(totalIn >>> 8);
            output.writeByte(totalIn >>> 16);
            output.writeByte(totalIn >>> 24);
            finished_ = true;
        }
    }
}
