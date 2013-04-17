package net.ihiroky.niotty.buffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class FileBufferSinkTest {

    private File file_;
    private FileChannel channel_;

    @Rule
    public TemporaryFolder temporaryFolderRule_ = new TemporaryFolder();

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        file_ = temporaryFolderRule_.newFile();
        channel_ = FileChannel.open(file_.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
        byte[] data = new byte[32];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        channel_.write(ByteBuffer.wrap(data));
        channel_.position(0);
    }

    @After
    public void tearDown() throws Exception {
        channel_.close();
    }

    private static class TransferAll implements Answer<Integer> {

        ByteBuffer written_ = ByteBuffer.allocate(32);

        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
            written_.put(bb);
            written_.flip();
            return bb.limit();
        }
    }

    private static class TransferPart implements Answer<Integer> {

        ByteBuffer written_;

        TransferPart(int value) {
            written_ = ByteBuffer.allocate(value);
        }

        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            ByteBuffer bb = (ByteBuffer) invocation.getArguments()[0];
            int limit = bb.limit();
            bb.limit(written_.limit());
            written_.put(bb);
            written_.flip();
            bb.limit(limit);
            return written_.limit();
        }
    }

    @Test
    public void testTransferTo_WriteAllAtOnce() throws Exception {
        GatheringByteChannel outputChannel = mock(GatheringByteChannel.class);
        when(outputChannel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new TransferAll());
        when(outputChannel.isOpen()).thenReturn(true);

        FileBufferSink sut = new FileBufferSink(channel_, 8, 24, Buffers.DEFAULT_PRIORITY);
        boolean actual = sut.transferTo(outputChannel);

        assertThat(actual, is(true));
        assertThat(sut.remainingBytes(), is(0));
        assertThat(channel_.isOpen(), is(false));
    }

    @Test
    public void testTransferTo_WritePart() throws Exception {
        GatheringByteChannel outputChannel = mock(GatheringByteChannel.class);
        when(outputChannel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new TransferPart(10)); // footer
        when(outputChannel.isOpen()).thenReturn(true);

        FileBufferSink sut = new FileBufferSink(channel_, 0, 15, Buffers.DEFAULT_PRIORITY);
        boolean actual = sut.transferTo(outputChannel);

        assertThat(actual, is(false));
        assertThat(sut.remainingBytes(), is(5));
        assertThat(channel_.isOpen(), is(true));
    }

    @Test
    public void testTransferTo_IOException() throws Exception {
        GatheringByteChannel outputChannel = mock(GatheringByteChannel.class);
        when(outputChannel.write(Mockito.any(ByteBuffer.class))).thenThrow(new IOException());
        when(outputChannel.isOpen()).thenReturn(true);

        FileBufferSink sut = new FileBufferSink(channel_, 0, 15, Buffers.DEFAULT_PRIORITY);
        try {
            sut.transferTo(outputChannel);
        } catch (IOException ioe) {
        }

        assertThat(sut.remainingBytes(), is(15));
        assertThat(channel_.isOpen(), is(false));
    }

    @Test
    public void testTransferTo_HeaderHalfway() throws Exception {
        CodecBuffer header = Buffers.newCodecBuffer(new byte[8], 0, 8);
        CodecBuffer footer = Buffers.newCodecBuffer(new byte[8], 0, 8);
        GatheringByteChannel outputChannel = mock(GatheringByteChannel.class);
        when(outputChannel.write(Mockito.any(ByteBuffer.class)))
                .thenAnswer(new TransferPart(header.remainingBytes() - 1));
        FileBufferSink sut = new FileBufferSink(channel_, 0, 32, Buffers.DEFAULT_PRIORITY);
        sut.addFirst(header).addLast(footer);

        boolean result = sut.transferTo(outputChannel);

        assertThat(result, is(false));
        assertThat(sut.remainingBytes(), is(1 + 32 + 8));
        verify(outputChannel, times(1)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testTransferTo_FooterHalfway() throws Exception {
        CodecBuffer header = Buffers.newCodecBuffer(new byte[8], 0, 8);
        CodecBuffer footer = Buffers.newCodecBuffer(new byte[8], 0, 8);
        GatheringByteChannel outputChannel = mock(GatheringByteChannel.class);
        when(outputChannel.isOpen()).thenReturn(true);
        when(outputChannel.write(Mockito.any(ByteBuffer.class)))
                .thenAnswer(new TransferAll())
                .thenAnswer(new TransferAll())
                .thenAnswer(new TransferPart(7));
        FileBufferSink sut = new FileBufferSink(channel_, 0, 32, Buffers.DEFAULT_PRIORITY);
        sut.addFirst(header).addLast(footer);

        boolean result = sut.transferTo(outputChannel);

        assertThat(result, is(false));
        assertThat(sut.remainingBytes(), is(0 + 0 + 1));
        verify(outputChannel, times(3)).write(Mockito.any(ByteBuffer.class));
    }

    @Test
    public void testPriority() throws Exception {
        FileBufferSink sut = new FileBufferSink(channel_, 0, 10, Buffers.DEFAULT_PRIORITY);
        assertThat(sut.priority(), is(-1));

        sut = new FileBufferSink(channel_, 0, 10, 0);
        assertThat(sut.priority(), is(0));
    }

    @Test
    public void testSlice() throws Exception {
        GatheringByteChannel outputChannel = mock(GatheringByteChannel.class);
        TransferAll all9 = new TransferAll();
        TransferAll all10 = new TransferAll();
        TransferAll allLeft = new TransferAll();
        when(outputChannel.isOpen())
                .thenReturn(true).thenReturn(true).thenReturn(true);
        when(outputChannel.write(Mockito.any(ByteBuffer.class)))
                .thenAnswer(all9)
                .thenAnswer(all10)
                .thenAnswer(allLeft);
        FileBufferSink sut = new FileBufferSink(channel_, 0, 32, 0);

        BufferSink sliced0 = sut.slice(9);
        sliced0.transferTo(outputChannel);
        boolean open0 = channel_.isOpen();
        BufferSink sliced1 = sut.slice(11);
        sliced1.transferTo(outputChannel);
        boolean open1 = channel_.isOpen();
        sut.transferTo(outputChannel);
        boolean openLeft = channel_.isOpen();

        assertThat(open0, is(true));
        assertContent(all9.written_, new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8});
        assertThat(sliced0.remainingBytes(), is(0));
        assertThat(sliced0.priority(), is(0));

        assertThat(open1, is(true));
        assertContent(all10.written_, new byte[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19});
        assertThat(sliced1.remainingBytes(), is(0));
        assertThat(sliced1.priority(), is(0));

        assertThat(openLeft, is(false));
        assertContent(allLeft.written_, new byte[]{20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31});
        assertThat(sut.remainingBytes(), is(0));
        assertThat(sut.priority(), is(0));
    }

    @Test
    public void testSlice_ExceedingBytes() throws Exception {
        FileBufferSink sut = new FileBufferSink(channel_, 0, 32, 0);
        sut.addFirst(Buffers.newCodecBuffer(new byte[1], 0, 1));
        sut.addLast(Buffers.newCodecBuffer(new byte[2],0, 2));
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("Invalid input 36. 35 byte remains.");

        sut.slice(36);
    }

    @Test
    public void testSlice_NegativeBytes() throws Exception {
        FileBufferSink sut = new FileBufferSink(channel_, 0, 32, 0);
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("Invalid input -1. 32 byte remains.");

        sut.slice(-1);
    }

    @Test
    public void testSlice_Empty() throws Exception {
        FileBufferSink sut = new FileBufferSink(channel_, 0, 0, 0);

        BufferSink sliced = sut.slice(0);

        assertThat(sliced.remainingBytes(), is(0));
    }

    @Test
    public void testSlice_HeaderOnly() throws Exception {
        FileBufferSink sut = new FileBufferSink(channel_, 0, 32, 0);
        byte[] header = new byte[3];
        Arrays.fill(header, (byte) 1);
        sut.addFirst(Buffers.newCodecBuffer(header, 0, header.length));
        byte[] footer = new byte[4];
        Arrays.fill(footer, (byte) -1);
        sut.addLast(Buffers.newCodecBuffer(footer, 0, footer.length));

        BufferSink sliced = sut.slice(header.length);

        assertThat(sliced.remainingBytes(), is(header.length));
        assertThat(sut.remainingBytes(), is(32 + footer.length));
        assertThat(sut.header().remainingBytes(), is(0));
        assertThat(sut.footer().remainingBytes(), is(footer.length));
    }

    @Test
    public void testSlice_HeaderAndContent() throws Exception {
        FileBufferSink sut = new FileBufferSink(channel_, 0, 32, 0);
        byte[] header = new byte[3];
        Arrays.fill(header, (byte) 1);
        sut.addFirst(Buffers.newCodecBuffer(header, 0, header.length));
        byte[] footer = new byte[4];
        Arrays.fill(footer, (byte) -1);
        sut.addLast(Buffers.newCodecBuffer(footer, 0, footer.length));

        BufferSink sliced = sut.slice(header.length + 32);

        assertThat(sliced.remainingBytes(), is(header.length + 32));
        assertThat(sut.remainingBytes(), is(footer.length));
        assertThat(sut.header().remainingBytes(), is(0));
        assertThat(sut.footer().remainingBytes(), is(footer.length));
    }

    @Test
    public void testSlice_HeaderContentAndFooter() throws Exception {
        FileBufferSink sut = new FileBufferSink(channel_, 0, 32, 0);
        byte[] header = new byte[3];
        Arrays.fill(header, (byte) 1);
        sut.addFirst(Buffers.newCodecBuffer(header, 0, header.length));
        byte[] footer = new byte[4];
        Arrays.fill(footer, (byte) -1);
        sut.addLast(Buffers.newCodecBuffer(footer, 0, footer.length));

        BufferSink sliced = sut.slice(header.length + 32 + footer.length);

        assertThat(sliced.remainingBytes(), is(header.length + 32 + footer.length));
        assertThat(sut.remainingBytes(), is(0));
        assertThat(sut.header().remainingBytes(), is(0));
        assertThat(sut.footer().remainingBytes(), is(0));
    }

    static void assertContent(ByteBuffer actual, byte[] expected) {
        assertThat("content length", actual.remaining(), is(expected.length));
        byte[] actualBytes = new byte[actual.remaining()];
        actual.get(actualBytes);
        assertThat("content", actualBytes, is(expected));
    }

    @Test
    public void testDispose() throws Exception {
        FileBufferSink sut = new FileBufferSink(channel_, 0, 10, 0);

        sut.dispose();

        assertThat(channel_.isOpen(), is(false));
    }

    @Test
    public void testAddFirst_Reference() throws Exception {
        FileBufferSink sut = new FileBufferSink(channel_, 0, 10, 0);
        byte[] data = new byte[4];
        Arrays.fill(data, (byte) 1);
        CodecBuffer added = Buffers.newCodecBuffer(data, 0, data.length);

        sut.addFirst(added);

        assertThat(sut.header(), is(added));
        assertThat(sut.footer().remainingBytes(), is(0));
    }

    @Test
    public void testAddFirst_Copy() throws Exception {
        FileBufferSink sut = new FileBufferSink(channel_, 0, 10, 0);
        byte[] data0 = new byte[2];
        Arrays.fill(data0, (byte) 1);
        CodecBuffer added0 = Buffers.newCodecBuffer(data0, 0, data0.length);
        byte[] data1 = new byte[2];
        Arrays.fill(data1, (byte) -1);
        CodecBuffer added1 = Buffers.newCodecBuffer(data1, 0, data1.length);

        sut.addFirst(added0);
        sut.addFirst(added1);

        assertThat(sut.header(), is(added0));
        assertThat(sut.header().toArray(), is(new byte[]{-1, -1, 1, 1}));
        assertThat(sut.footer().remainingBytes(), is(0));
    }

    @Test
    public void testAddLast_Reference() throws Exception {
        FileBufferSink sut = new FileBufferSink(channel_, 0, 10, 0);
        byte[] data = new byte[4];
        Arrays.fill(data, (byte) 1);
        CodecBuffer added = Buffers.newCodecBuffer(data, 0, data.length);

        sut.addLast(added);

        assertThat(sut.header().remainingBytes(), is(0));
        assertThat(sut.footer(), is(added));
    }

    @Test
    public void testAddLast_Copy() throws Exception {
        FileBufferSink sut = new FileBufferSink(channel_, 0, 10, 0);
        byte[] data0 = new byte[2];
        Arrays.fill(data0, (byte) 1);
        CodecBuffer added0 = Buffers.newCodecBuffer(data0, 0, data0.length);
        byte[] data1 = new byte[2];
        Arrays.fill(data1, (byte) -1);
        CodecBuffer added1 = Buffers.newCodecBuffer(data1, 0, data1.length);

        sut.addLast(added0);
        sut.addLast(added1);

        assertThat(sut.header().remainingBytes(), is(0));
        assertThat(sut.footer(), is(added0));
        assertThat(sut.footer().toArray(), is(new byte[]{1, 1, -1, -1}));
    }
}
