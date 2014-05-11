package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.DefaultEventDispatcher;
import net.ihiroky.niotty.DefaultEventDispatcherGroup;
import net.ihiroky.niotty.Event;
import net.ihiroky.niotty.EventDispatcher;
import net.ihiroky.niotty.EventDispatcherGroup;
import net.ihiroky.niotty.EventDispatcherSelection;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.Packet;
import net.ihiroky.niotty.util.Arguments;
import net.ihiroky.niotty.util.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Provides interface to write {@link net.ihiroky.niotty.buffer.Packet} into a file.
 *
 * <dl>
 *     <dt>Buffering</dt>
 *     <dd>
 *         The input data is once written into an internal buffer and then the buffer
 *         is flush into the file. This operation is relied on
 *         {@link net.ihiroky.niotty.codec.BufferedGatheringByteChannel}.
 *     </dd>
 *     <dt>Auto flush</dt>
 *     <dd>
 *         The internal buffer is flush automatically if {@code flushIntervalSeconds}
 *         is set at the constructor. The auto flush is disabled when the
 *         {@code flushIntervalSeconds} is zero or negative.
 *     </dd>
 *     <dt>Rollover</dt>
 *     <dd>
 *         The file is rolled over if the file size exceeds a threshold or it's time
 *         to be rolled over. These parameters are set at the constructor using
 *         {@link net.ihiroky.niotty.codec.RolloverOption}.
 *     </dd>
 *     <dt>Decoration</dt>
 *     <dd>
 *         The packet can be written into the file with the timestamp, the size
 *         of the packet and a separator. These are controlled by
 *         {@link net.ihiroky.niotty.codec.DecorationOption} specified by the
 *         constructor.
 *     </dd>
 * </dl>
 */
public class FilePacketWriter implements PacketWriter, EventDispatcherSelection {

    private final EventDispatcherGroup<DefaultEventDispatcher> dispatcherGroup_;
    private final EventDispatcher dispatcher_;
    private final String path_;
    private final FileChannel channel_;
    private final BufferedGatheringByteChannel bufferedChannel_;
    private final RolloverOption rolloverOption_;
    private final DecorationOption decorationOption_;
    private final DateFormat dateFormat_;

    private static final Logger LOG = LoggerFactory.getLogger(FilePacketWriter.class);

    /**
     * Constructs a new instance.
     *
     * @param path the path of the file
     * @param bufferedChannel a channel used for buffering
     * @param flushIntervalSeconds the interval of the auto flush
     * @param rolloverOption the option to specify rollover operation
     * @param decorationOption the option to define decorations
     * @throws java.io.IOException if an I/O error occur
     * @throws java.lang.NullPointerException if some arguments are null
     */
    public FilePacketWriter(String path, BufferedGatheringByteChannel bufferedChannel,
            final int flushIntervalSeconds, RolloverOption rolloverOption, DecorationOption decorationOption)
            throws IOException {
        path_ = Arguments.requireNonNull(path, "path");
        channel_ = new RandomAccessFile(path, "rw").getChannel();
        channel_.position(channel_.size());
        dateFormat_ = new SimpleDateFormat("yyyyMMdd-HHmmss");
        bufferedChannel_ = Arguments.requireNonNull(bufferedChannel, "bufferedChannel");
        bufferedChannel_.setUnderlyingChannel(channel_);
        dispatcherGroup_ = new DefaultEventDispatcherGroup(1, new NameCountThreadFactory("Journal:" + path));
        dispatcher_ = dispatcherGroup_.assign(this);

        rolloverOption_ = Arguments.requireNonNull(rolloverOption, "rolloverOption");
        decorationOption_ = Arguments.requireNonNull(decorationOption, "recordModifierOption");


        if (flushIntervalSeconds > 0) {
            dispatcher_.schedule(new Event() {
                @Override
                public long execute() throws Exception {
                    flush();
                    return TimeUnit.SECONDS.toNanos(flushIntervalSeconds);
                }
            }, flushIntervalSeconds, TimeUnit.SECONDS);
        }
    }

    void flush() {
        BufferedGatheringByteChannel c = bufferedChannel_;
        try {
            c.flush();
        } catch (IOException ioe) {
            LOG.warn("[execute] Failed to flush " + path_, ioe);
        }
    }

    @Override
    public void write(final Packet packet) {
        dispatcher_.offer(new Event() {
            @Override
            public long execute() throws Exception {
                writeDirect(packet);
                return DONE;
            }
        });
    }

    // TODO timed rollover before writing.
    void writeDirect(final Packet packet) throws IOException {
        DecorationOption decorationOption = decorationOption_;
        int addSize = packet.remaining() + decorationOption.size();
        long now = currentTimeMillis();
        if (rolloverOption_.requiresRollover(addSize, now)) {
            rollOverDirect();
        }

        BufferedGatheringByteChannel channel = bufferedChannel_;
        decorationOption.prepend(channel, addSize, now);
        packet.sink(channel);
        packet.dispose();
        decorationOption.append(channel, addSize, now);
    }

    @Override
    public void rollOver() {
        dispatcher_.offer(new Event() {
            @Override
            public long execute() throws Exception {
                rollOverDirect();
                return DONE;
            }
        });
    }

    void rollOverDirect() throws IOException {
        BufferedGatheringByteChannel bufferedChannel = bufferedChannel_;
        bufferedChannel.flush();
        bufferedChannel.closeUnderlyingChannel();

        File originalFile = new File(path_);
        File movedFile = new File(path_ + '.' + dateFormat_.format(new Date(currentTimeMillis())));
        if (!originalFile.renameTo(movedFile)) {
            LOG.warn("[rollOver] Failed to rename file from {} to {}.", originalFile, movedFile);
        }
        FileChannel newChannel = new RandomAccessFile(path_, "rw").getChannel();
        bufferedChannel.setUnderlyingChannel(newChannel);
    }

    @Override
    public void close() {
        dispatcherGroup_.close();
        try {
            bufferedChannel_.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            bufferedChannel_.close();
        }

        try {
            channel_.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Test method.
     * @param args -
     */
    public static void main(String[] args) {
        FilePacketWriter djw = null;
        try {
            djw = new FilePacketWriter("/tmp/DefaultJournalWriterTest.log",
                    new BufferedGatheringByteChannel(128, true), 1,
                    new RolloverOption(Long.MAX_VALUE, Collections.singleton(new RolloverOption.Time(16, 07, 0))),
                    new DecorationOption(true, false, new byte[]{'\n'}));
            for (int count = 0;; count++) {
                Packet packet = Buffers.wrap(String.format("%05d", count).getBytes(Charsets.UTF_8));
                djw.write(packet);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (djw != null) {
                djw.close();
            }
        }
    }
}
