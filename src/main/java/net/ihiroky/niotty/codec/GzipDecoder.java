package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;

import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * @author Hiroki Itoh
 */
public class GzipDecoder extends InflaterDecoder {

    private CRC32 crc32_;
    private GzipHeaderParser headerParser_;
    private GzipFooterParser footerParser_;

    public GzipDecoder() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public GzipDecoder(int bufferSize) {
        super(bufferSize, null, true);
        crc32_ = new CRC32();
        headerParser_ = new GzipHeaderParser();
    }

    protected int onBeforeDecode(byte[] output, int offset, int length) throws DataFormatException {
        if (headerParser_ != null) {
            int n = headerParser_.parse(Buffers.wrap(output, offset, length), crc32_);
            if (n != -1) {
                headerParser_ = null;
            }
            return n;
        }
        return 0;
    }

    @Override
    protected void onAfterDecode(byte[] output, int offset, int length) throws DataFormatException {
        crc32_.update(output, offset, length);
    }

    @Override
    protected void onAfterFinished(CodecBuffer input, Inflater inflater) throws DataFormatException {
        if (footerParser_ == null) {
            footerParser_ = new GzipFooterParser(input, inflater.getBytesWritten(), crc32_);
        }
        int n = footerParser_.readTrailer();
        if (n != -1) {
            footerParser_ = null;
        }
    }

    private static class GzipHeaderParser {

        private InputSupport inputSupport_;
        private int length_;
        private State state_;
        private int flag_;
        private int value_;

        private enum State {
            MAGIC(2),
            METHOD(1),
            FLAG(1),
            MTIME_XFL_OS(6),
            FEXTRA(2),
            FEXTRA_CONTENT(-1),
            FNAME(-1),
            FCOMMENT(-1),
            FHCRC(2),
            END(-1)
            ;

            final int length_;

            State(int length) {
                length_ = length;
            }
        }

        /**
         * GZIP header magic number.
         */
        public final static int GZIP_MAGIC = 0x8b1f;

        /*
         * File header flags.
         */
        private final static int FTEXT      = 1;    // Extra text
        private final static int FHCRC      = 2;    // Header CRC
        private final static int FEXTRA     = 4;    // Extra field
        private final static int FNAME      = 8;    // File name
        private final static int FCOMMENT   = 16;   // File comment

        private static final int MASK_SHORT = 0xFFFF;

        GzipHeaderParser() {
            state_ = State.MAGIC;
            inputSupport_ = new InputSupport();
        }

        /*
         * Reads GZIP member header and returns the total byte number of this member header.
         */
        int parse(final CodecBuffer input, final CRC32 crc) throws DataFormatException {
            CodecBuffer b;
            int beginning = input.beginning();
            switch (state_) {
                case MAGIC:
                    b = inputSupport_.readFully(input, state_.length_, true);
                    if (b == null) {
                        return -1;
                    }
                    int magic = readUnsignedShort(b, crc);
                    if (magic != GZIP_MAGIC) {
                        throw new DataFormatException("Invalid magic number: " + Integer.toHexString(magic));
                    }

                    length_ += state_.length_;
                    state_ = State.METHOD;
                    // fall through
                case METHOD:
                    b = inputSupport_.readFully(input, state_.length_, true);
                    if (b == null) {
                        return -1;
                    }
                    int method = readUnsignedByte(b, crc);
                    if (method != Deflater.DEFLATED) {
                        throw new DataFormatException("Invalid method: " + method);
                    }
                    length_ += state_.length_;
                    state_ = State.FLAG;
                    // fall through
                case FLAG:
                    b = inputSupport_.readFully(input, state_.length_, true);
                    if (b == null) {
                        return -1;
                    }
                    flag_ = readUnsignedByte(b, crc);
                    length_ += state_.length_;
                    state_ = State.MTIME_XFL_OS;
                    // fall through
                case MTIME_XFL_OS:
                    b = inputSupport_.readFully(input, state_.length_, true);
                    if (b == null) {
                        return -1;
                    }
                    for (int i = 0; i < state_.length_; i++) {
                        readUnsignedByte(b, crc);
                    }
                    length_ += state_.length_;
                    state_ = State.FEXTRA;
                    // fall through
                case FEXTRA:
                    if ((flag_ & FEXTRA) == FEXTRA) {
                        b = inputSupport_.readFully(input, state_.length_, true);
                        if (b == null) {
                            return -1;
                        }
                        value_ = readUnsignedShort(b, crc);
                        length_ += state_.length_;
                    }
                    state_ = State.FEXTRA_CONTENT;
                    // fall through
                case FEXTRA_CONTENT:
                    if ((flag_ & FEXTRA) == FEXTRA) {
                        b = inputSupport_.readFully(input, value_, true);
                        if (b == null) {
                            return -1;
                        }
                        for (int i = 0; i < value_; i++) {
                            readUnsignedByte(b, crc);
                        }
                        length_ += state_.length_;
                    }
                    state_ = State.FNAME;
                    // fall through
                case FNAME:
                    if ((flag_ & FNAME)  == FNAME) {
                        int nameEndIndex = input.indexOf(0, 0);
                        if (nameEndIndex == -1) {
                            int remaining = input.remainingBytes();
                            for (int i = 0; i <= remaining; i++) {
                                readUnsignedByte(input, crc);
                            }
                            length_ += remaining;
                            return -1;
                        }
                        for (int i = 0; i <= nameEndIndex; i++) {
                            readUnsignedByte(input, crc);
                        }
                        length_ += nameEndIndex + 1;
                    }
                    state_ = State.FCOMMENT;
                    // fall through
                case FCOMMENT:
                    if ((flag_ & FCOMMENT) == FCOMMENT) {
                        int commentEndIndex = input.indexOf(0, 0);
                        if (commentEndIndex == -1) {
                            int remaining = input.remainingBytes();
                            for (int i = 0; i <= remaining; i++) {
                                readUnsignedByte(input, crc);
                            }
                            length_ += remaining;
                            return -1;
                        }
                        for (int i = 0; i <= commentEndIndex; i++) {
                            readUnsignedByte(input, crc);
                        }
                        length_ += commentEndIndex + 1;
                    }
                    state_ = State.FHCRC;
                    // fall through
                case FHCRC:
                    if ((flag_ & FHCRC) == FHCRC) {
                        b = inputSupport_.readFully(input, state_.length_, true);
                        if (b == null) {
                            return -1;
                        }
                        int v = (int) crc.getValue() & MASK_SHORT;
                        int rv = readUnsignedShort(b, crc);
                        if (readUnsignedShort(b, crc) != v) {
                            throw new DataFormatException("Crc check is failed. value:" + rv + ", calculated: " + v);
                        }
                        length_ += state_.length_;
                    }
                    crc.reset();
                    state_ = State.END;
                    // fall through
                default:
            }
            return input.beginning() - beginning;
        }

        /*
         * Reads unsigned integer in Intel byte order.
         */
        private long readUnsignedInt(CodecBuffer buffer, CRC32 crc32) {
            long s = readUnsignedShort(buffer, crc32);
            return ((long) readUnsignedShort(buffer, crc32) << Short.SIZE) | s;
        }

        /*
         * Reads unsigned short in Intel byte order.
         */
        private int readUnsignedShort(CodecBuffer buffer, CRC32 crc32) {
            int b = readUnsignedByte(buffer, crc32);
            return (readUnsignedByte(buffer, crc32) << Byte.SIZE) | b;
        }

        /*
         * Reads unsigned byte.
         */
        private int readUnsignedByte(CodecBuffer buffer, CRC32 crc32) {
            int b = buffer.readByte();
            crc32.update(b);
            return b;
        }
    }

    private static class GzipFooterParser {

        private long bytesWritten_;
        private CRC32 crc_;
        private CodecBuffer input_;
        private InputSupport inputSupport_;

        private static final int TRAILER_LENGTH = 8;
        private static final long MASK_32 = 0xFFFFFFFFL;

        private GzipFooterParser(CodecBuffer input, long bytesWritten, CRC32 crc) {
            input_ = input;
            bytesWritten_ = bytesWritten;
            crc_ = crc;
            inputSupport_ = new InputSupport();
        }

        int readTrailer() throws DataFormatException {
            CodecBuffer b = inputSupport_.readFully(input_, TRAILER_LENGTH, true);
            if (b == null) {
                return -1;
            }
            long crcValue = readUnsignedInt(b);
            long written = readUnsignedInt(b);
            if (crcValue != crc_.getValue()
                    || written != (bytesWritten_ & MASK_32)) {
                throw new DataFormatException("Corrupt GZIP trailer");
            }
            return TRAILER_LENGTH;
        }

        /*
         * Reads unsigned integer in Intel byte order.
         */
        private long readUnsignedInt(CodecBuffer buffer) {
            long s = readUnsignedShort(buffer);
            return ((long) readUnsignedShort(buffer) << Short.SIZE) | s;
        }

        /*
         * Reads unsigned short in Intel byte order.
         */
        private int readUnsignedShort(CodecBuffer buffer) {
            int b = buffer.readByte();
            return (buffer.readByte() << Byte.SIZE) | b;
        }
    }
}
