package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.buffer.CodecBuffer;

import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;

/**
 * @author Hiroki Itoh
 */
public class GzipEncoder extends DeflaterEncoder {

    private final CRC32 crc32_;
    private boolean first_;
    private boolean finished_;

    private static final int GZIP_MAGIC = GZIPInputStream.GZIP_MAGIC;

    private static final byte[] GZIP_HEADER = new byte[]{
            (byte) GZIP_MAGIC,        // Magic number (short)
            (byte) (GZIP_MAGIC >> Byte.SIZE),  // Magic number (short)
            Deflater.DEFLATED,        // Compression method (CM)
            0,                        // Flags (FLG)
            0,                        // Modification time MTIME (int)
            0,                        // Modification time MTIME (int)
            0,                        // Modification time MTIME (int)
            0,                        // Modification time MTIME (int)
            0,                        // Extra flags (XFLG)
            0                         // Operating system (OS)
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
    protected void onBeforeEncode(byte[] input, int offset, int length, CodecBuffer output) {
        if (first_) {
            output.writeBytes(GZIP_HEADER, 0, GZIP_HEADER.length);
            first_ = false;
        }
        crc32_.update(input, offset, length);
    }

    @Override
    protected void onAfterFinished(CodecBuffer output, Deflater deflater) {
        if (!finished_) {
            // write crc and totalIn with little endian
            int crc = (int) crc32_.getValue();
            output.writeByte(crc);
            output.writeByte(crc >>> Byte.SIZE);
            output.writeByte(crc >>> Short.SIZE);
            output.writeByte(crc >>> (Byte.SIZE + Short.SIZE));
            int totalIn = deflater.getTotalIn();
            output.writeByte(totalIn);
            output.writeByte(totalIn >>> Byte.SIZE);
            output.writeByte(totalIn >>> Short.SIZE);
            output.writeByte(totalIn >>> (Byte.SIZE + Short.SIZE));
            finished_ = true;
        }
    }
}
