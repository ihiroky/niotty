package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.Packet;
import net.ihiroky.niotty.util.Charsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class FilePacketWriterTest {

    private FilePacketWriter sut_;
    private File file_;
    private BufferedGatheringByteChannel bufferedChannel_;

    @Rule
    public TemporaryFolder temporaryFolder_ = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        file_ = temporaryFolder_.newFile();
        bufferedChannel_ = new BufferedGatheringByteChannel(32, false);
        DecorationOption decorationOption = new DecorationOption(true, false, new byte[]{'\n'});
        sut_ = new FilePacketWriter(file_.getPath(), bufferedChannel_, 0,
                new RolloverOption(128), decorationOption) {
            @Override
            protected long currentTimeMillis() {
                return 1000L;
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        sut_.close();
    }

    @Test
    public void testWrite() throws Exception {
        byte[] data = new byte[3];
        Arrays.fill(data, (byte) '0');
        Packet packet = Buffers.wrap(data);

        sut_.writeDirect(packet);

        String expected = "19700101-090001.000 000\n";
        ByteBuffer actualBytes = bufferedChannel_.buffer_;
        actualBytes.flip();
        String actual = Charsets.UTF_8.newDecoder().decode(actualBytes).toString();
        assertThat(expected, is(actual));
    }

    @Test
    public void testRollover() throws Exception {
        byte[] data0 = new byte[128 - 20 - 1 - 1]; // 20:timestamp, 1:separator
        Arrays.fill(data0, (byte) '0');
        Packet p0 = Buffers.wrap(data0);
        byte[] data1 = new byte[128 - 20 - 1 - 1]; // 20:timestamp, 1:separator
        Arrays.fill(data1, (byte) '1');
        Packet p1 = Buffers.wrap(data1);

        sut_.writeDirect(p0);
        sut_.writeDirect(p1);

        File rolloveredFile = new File(file_.getPath() + ".19700101-090001");
        assertThat(file_.exists(), is(true));
        assertThat(file_.length(), is(96L)); // 31 bytes is still buffered.
        assertThat(bufferedChannel_.buffer_.flip().remaining(), is(31));
        assertThat(rolloveredFile.exists(), is(true));
        assertThat(rolloveredFile.length(), is(127L));
    }
}
