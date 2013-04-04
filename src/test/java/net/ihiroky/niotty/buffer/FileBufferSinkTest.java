package net.ihiroky.niotty.buffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;

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
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        file_ = temporaryFolder.newFile();
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
        WritableByteChannel outputChannel = mock(WritableByteChannel.class);
        when(outputChannel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new TransferAll());
        when(outputChannel.isOpen()).thenReturn(true);

        FileBufferSink sut = new FileBufferSink(channel_, 8, 24, Buffers.DEFAULT_PRIORITY);
        boolean actual = sut.transferTo(outputChannel, ByteBuffer.allocate(0));

        assertThat(actual, is(true));
        assertThat(sut.remainingBytes(), is(0));
        assertThat(channel_.isOpen(), is(false));
    }

    @Test
    public void testTransferTo_WritePart() throws Exception {
        WritableByteChannel outputChannel = mock(WritableByteChannel.class);
        when(outputChannel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new TransferPart(10));
        when(outputChannel.isOpen()).thenReturn(true);

        FileBufferSink sut = new FileBufferSink(channel_, 0, 15, Buffers.DEFAULT_PRIORITY);
        boolean actual = sut.transferTo(outputChannel, ByteBuffer.allocate(0));

        assertThat(actual, is(false));
        assertThat(sut.remainingBytes(), is(5));
        assertThat(channel_.isOpen(), is(true));
    }

    @Test
    public void testTransferTo_IOException() throws Exception {
        WritableByteChannel outputChannel = mock(WritableByteChannel.class);
        when(outputChannel.write(Mockito.any(ByteBuffer.class))).thenThrow(new IOException());
        when(outputChannel.isOpen()).thenReturn(true);

        FileBufferSink sut = new FileBufferSink(channel_, 0, 15, Buffers.DEFAULT_PRIORITY);
        try {
            sut.transferTo(outputChannel, ByteBuffer.allocate(0));
        } catch (IOException ioe) {
        }

        assertThat(sut.remainingBytes(), is(15));
        assertThat(channel_.isOpen(), is(false));
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
        WritableByteChannel outputChannel = mock(WritableByteChannel.class);
        TransferAll all9 = new TransferAll();
        TransferAll all10 = new TransferAll();
        TransferAll allLeft = new TransferAll();
        when(outputChannel.isOpen())
                .thenReturn(true).thenReturn(true).thenReturn(true);
        when(outputChannel.write(Mockito.any(ByteBuffer.class)))
                .thenAnswer(all9).thenAnswer(all10).thenAnswer(allLeft);
        FileBufferSink sut = new FileBufferSink(channel_, 0, 32, 0);

        FileBufferSink sliced0 = sut.slice(9);
        sliced0.transferTo(outputChannel, ByteBuffer.allocate(0));
        boolean open0 = channel_.isOpen();
        FileBufferSink sliced1 = sut.slice(11);
        sliced1.transferTo(outputChannel, ByteBuffer.allocate(0));
        boolean open1 = channel_.isOpen();
        sut.transferTo(outputChannel, ByteBuffer.allocate(0));
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
}
