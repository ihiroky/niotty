package net.ihiroky.niotty.buffer;

import net.ihiroky.niotty.util.Closable;
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
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class FilePacketTest {

    private FileChannel channel_;
    private List<ByteBufferChunkPool> closeableList;
    private byte[] originalData_;

    @Rule
    public TemporaryFolder temporaryFolderRule_ = new TemporaryFolder();

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        File file = temporaryFolderRule_.newFile();
        channel_ = new RandomAccessFile(file, "rw").getChannel();
        byte[] data = new byte[32];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        channel_.write(ByteBuffer.wrap(data));
        channel_.position(0);
        closeableList = new ArrayList<ByteBufferChunkPool>();
        originalData_ = data;
    }

    @After
    public void tearDown() throws Exception {
        for (Closable closeable : closeableList) {
            closeable.close();
        }
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

    private static class TransferAllForGathering implements Answer<Integer> {

        ByteBuffer written_ = ByteBuffer.allocate(32);

        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            ByteBuffer[] bs = (ByteBuffer[]) invocation.getArguments()[0];
            int read = 0;
            for (ByteBuffer b : bs) {
                read += b.remaining();
                written_.put(b);
            }
            written_.flip();
            return read;
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

    private static class TransferPartForGathering implements Answer<Integer> {

        ByteBuffer written_;

        TransferPartForGathering(int value) {
            written_ = ByteBuffer.allocate(value);
        }

        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            ByteBuffer[] bb = (ByteBuffer[]) invocation.getArguments()[0];
            for (ByteBuffer b : bb) {
                if (written_.remaining() >= b.remaining()) {
                    written_.put(b);
                } else {
                    int limit = b.limit();
                    b.limit(b.position() + written_.remaining());
                    written_.put(b);
                    b.limit(limit);
                    break;
                }
            }
            written_.flip();
            return written_.limit();
        }
    }

    @Test
    public void testSink_WriteAllAtOnce() throws Exception {
        GatheringByteChannel outputChannel = mock(GatheringByteChannel.class);
        when(outputChannel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new TransferAll());
        when(outputChannel.isOpen()).thenReturn(true);

        FilePacket sut = new FilePacket(channel_, 8, 24);
        boolean actual = sut.sink(outputChannel);
        boolean openAfterTransferTo = channel_.isOpen();
        sut.dispose();
        boolean openAfterDisposed = channel_.isOpen();

        assertThat(actual, is(true));
        assertThat(sut.remaining(), is(0));
        assertThat(openAfterTransferTo, is(true));
        assertThat(openAfterDisposed, is(false));
    }

    @Test
    public void testSink_WritePart() throws Exception {
        GatheringByteChannel outputChannel = mock(GatheringByteChannel.class);
        when(outputChannel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new TransferPart(10)); // footer
        when(outputChannel.isOpen()).thenReturn(true);

        FilePacket sut = new FilePacket(channel_, 0, 15);
        boolean actual = sut.sink(outputChannel);

        assertThat(actual, is(false));
        assertThat(sut.remaining(), is(5));
        assertThat(channel_.isOpen(), is(true));
    }

    @Test
    public void testSink_IOException() throws Exception {
        GatheringByteChannel outputChannel = mock(GatheringByteChannel.class);
        when(outputChannel.write(Mockito.any(ByteBuffer.class))).thenThrow(new IOException());
        when(outputChannel.isOpen()).thenReturn(true);

        FilePacket sut = new FilePacket(channel_, 0, 15);
        try {
            sut.sink(outputChannel);
        } catch (IOException ioe) {
        }

        assertThat(sut.remaining(), is(15));
        assertThat(channel_.isOpen(), is(false));
    }

    @Test
    public void testSink_HeaderHalfway() throws Exception {
        CodecBuffer header = Buffers.wrap(new byte[8], 0, 8);
        CodecBuffer footer = Buffers.wrap(new byte[8], 0, 8);
        GatheringByteChannel outputChannel = mock(GatheringByteChannel.class);
        when(outputChannel.write(Mockito.any(ByteBuffer[].class), anyInt(), anyInt()))
                .thenAnswer(new TransferPartForGathering(header.remaining() - 1));
        FilePacket sut = new FilePacket(channel_, 0, 32);
        sut.addFirst(header).addLast(footer);

        boolean result = sut.sink(outputChannel);

        assertThat(result, is(false));
        assertThat(sut.remaining(), is(1 + 32 + 8));
        verify(outputChannel, times(1)).write(Mockito.any(ByteBuffer[].class), anyInt(), anyInt());
    }

    @Test
    public void testSink_FooterHalfway() throws Exception {
        CodecBuffer header = Buffers.wrap(new byte[8], 0, 8);
        CodecBuffer footer = Buffers.wrap(new byte[8], 0, 8);
        GatheringByteChannel outputChannel = mock(GatheringByteChannel.class);
        when(outputChannel.isOpen()).thenReturn(true);
        when(outputChannel.write(Mockito.any(ByteBuffer[].class), anyInt(), anyInt()))
                .thenAnswer(new TransferAllForGathering())
                .thenAnswer(new TransferPartForGathering(7));
        when(outputChannel.write(Mockito.any(ByteBuffer.class)))
                .thenAnswer(new TransferAll());
        FilePacket sut = new FilePacket(channel_, 0, 32);
        sut.addFirst(header).addLast(footer);

        boolean result = sut.sink(outputChannel);

        assertThat(result, is(false));
        assertThat(sut.remaining(), is(0 + 0 + 1));
        verify(outputChannel, times(1)).write(Mockito.any(ByteBuffer.class));
        verify(outputChannel, times(2)).write(Mockito.any(ByteBuffer[].class), anyInt(), anyInt());
    }

    @Test
    public void testSinkDatagram_FlushAll() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 32);
        sut.addFirst(Buffers.wrap(new byte[8], 0, 8));
        sut.addLast(Buffers.wrap(new byte[8], 0, 8));
        DatagramChannel channel = mock(DatagramChannel.class);
        ByteBuffer buffer = ByteBuffer.allocate(48);
        SocketAddress target = new InetSocketAddress(12345);
        when(channel.send(buffer, target)).thenReturn(48);

        boolean result = sut.sink(channel, buffer, target);

        assertThat(result, is(true));
        assertThat(buffer.remaining(), is(buffer.capacity()));
        verify(channel).send(buffer, target);
    }

    @Test
    public void testSinkDatagram_FlushNothing() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 32);
        sut.addFirst(Buffers.wrap(new byte[8], 0, 8));
        sut.addLast(Buffers.wrap(new byte[8], 0, 8));
        DatagramChannel channel = mock(DatagramChannel.class);
        ByteBuffer buffer = ByteBuffer.allocate(48);
        SocketAddress target = new InetSocketAddress(12345);
        when(channel.send(buffer, target)).thenReturn(0);

        boolean result = sut.sink(channel, buffer, target);

        assertThat(result, is(false));
        assertThat(buffer.remaining(), is(buffer.capacity()));
        verify(channel).send(buffer, target);
    }

    @Test
    public void testCopyTo_ByteBufferAll() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 32);
        ByteBuffer buffer = ByteBuffer.allocate(sut.remaining());

        sut.copyTo(buffer);

        assertThat(sut.remaining(), is(32)); // remaining all
        assertThat(channel_.position(), is(0L));
        assertThat(buffer.array(), is(originalData_));
    }

    @Test(expected = BufferOverflowException.class)
    public void testCopyTo_ByteBufferPart() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 32);
        ByteBuffer buffer = ByteBuffer.allocate(sut.remaining() - 1);

        sut.copyTo(buffer);
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
        FilePacket sut = new FilePacket(channel_, 0, 32);

        Packet sliced0 = sut.slice(9);
        sliced0.sink(outputChannel);
        boolean open0 = channel_.isOpen();
        Packet sliced1 = sut.slice(11);
        sliced1.sink(outputChannel);
        boolean open1 = channel_.isOpen();
        sut.sink(outputChannel);
        boolean openLeft = channel_.isOpen();
        sliced0.dispose();
        boolean openDisposed0 = channel_.isOpen();
        sliced1.dispose();
        boolean openDisposed1 = channel_.isOpen();
        sut.dispose();
        boolean openDisposedLeft = channel_.isOpen();

        assertThat(open0, is(true));
        assertContent(all9.written_, new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8});
        assertThat(sliced0.remaining(), is(0));

        assertThat(open1, is(true));
        assertContent(all10.written_, new byte[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19});
        assertThat(sliced1.remaining(), is(0));

        assertThat(openLeft, is(true));
        assertContent(allLeft.written_, new byte[]{20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31});
        assertThat(sut.remaining(), is(0));

        assertThat(openDisposed0, is(true));
        assertThat(openDisposed1, is(true));
        assertThat(openDisposedLeft, is(false));
    }

    @Test
    public void testSlice_ExceedingBytes() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 32);
        sut.addFirst(Buffers.wrap(new byte[1], 0, 1));
        sut.addLast(Buffers.wrap(new byte[2], 0, 2));
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("Invalid input 36. 35 byte remains.");

        sut.slice(36);
    }

    @Test
    public void testSlice_NegativeBytes() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 32);
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("Invalid input -1. 32 byte remains.");

        sut.slice(-1);
    }

    @Test
    public void testSlice_Empty() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 0);

        Packet sliced = sut.slice(0);

        assertThat(sliced.remaining(), is(0));
    }

    @Test
    public void testSlice_HeaderOnly() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 32);
        byte[] header = new byte[3];
        Arrays.fill(header, (byte) 1);
        sut.addFirst(Buffers.wrap(header, 0, header.length));
        byte[] footer = new byte[4];
        Arrays.fill(footer, (byte) -1);
        sut.addLast(Buffers.wrap(footer, 0, footer.length));

        Packet sliced = sut.slice(header.length);

        assertThat(sliced.remaining(), is(header.length));
        assertThat(sut.remaining(), is(32 + footer.length));
        assertThat(sut.header().remaining(), is(0));
        assertThat(sut.footer().remaining(), is(footer.length));
    }

    @Test
    public void testSlice_HeaderAndContent() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 32);
        byte[] header = new byte[3];
        Arrays.fill(header, (byte) 1);
        sut.addFirst(Buffers.wrap(header, 0, header.length));
        byte[] footer = new byte[4];
        Arrays.fill(footer, (byte) -1);
        sut.addLast(Buffers.wrap(footer, 0, footer.length));

        Packet sliced = sut.slice(header.length + 32);

        assertThat(sliced.remaining(), is(header.length + 32));
        assertThat(sut.remaining(), is(footer.length));
        assertThat(sut.header().remaining(), is(0));
        assertThat(sut.footer().remaining(), is(footer.length));
    }

    @Test
    public void testSlice_HeaderContentAndFooter() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 32);
        byte[] header = new byte[3];
        Arrays.fill(header, (byte) 1);
        sut.addFirst(Buffers.wrap(header, 0, header.length));
        byte[] footer = new byte[4];
        Arrays.fill(footer, (byte) -1);
        sut.addLast(Buffers.wrap(footer, 0, footer.length));

        Packet sliced = sut.slice(header.length + 32 + footer.length);

        assertThat(sliced.remaining(), is(header.length + 32 + footer.length));
        assertThat(sut.remaining(), is(0));
        assertThat(sut.header().remaining(), is(0));
        assertThat(sut.footer().remaining(), is(0));
    }

    static void assertContent(ByteBuffer actual, byte[] expected) {
        assertThat("content length", actual.remaining(), is(expected.length));
        byte[] actualBytes = new byte[actual.remaining()];
        actual.get(actualBytes);
        assertThat("content", actualBytes, is(expected));
    }

    @Test
    public void testDispose() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 10);

        sut.dispose();

        assertThat(channel_.isOpen(), is(false));
    }

    @Test
    public void testAddFirst_One() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 10);
        byte[] data = new byte[4];
        Arrays.fill(data, (byte) 1);
        CodecBuffer added = Buffers.wrap(data, 0, data.length);

        sut.addFirst(added);

        byte[] headerContent = new byte[sut.header().remaining()];
        sut.header().readBytes(headerContent, 0, headerContent.length);
        assertThat(headerContent, is(data));
        assertThat(sut.header().remaining(), is(0));
        assertThat(sut.footer().remaining(), is(0));
    }

    @Test
    public void testAddFirst_Two() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 10);
        byte[] data0 = new byte[2];
        Arrays.fill(data0, (byte) 1);
        CodecBuffer added0 = Buffers.wrap(data0, 0, data0.length);
        byte[] data1 = new byte[2];
        Arrays.fill(data1, (byte) -1);
        CodecBuffer added1 = Buffers.wrap(data1, 0, data1.length);

        sut.addFirst(added0);
        sut.addFirst(added1);

        byte[] headerContent = new byte[sut.header().remaining()];
        sut.header().readBytes(headerContent, 0, headerContent.length);
        assertThat(headerContent, is(new byte[]{-1, -1, 1, 1}));
        assertThat(sut.footer().remaining(), is(0));
    }

    @Test
    public void testAddLast_One() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 10);
        byte[] data = new byte[4];
        Arrays.fill(data, (byte) 1);
        CodecBuffer added = Buffers.wrap(data, 0, data.length);

        sut.addLast(added);

        assertThat(sut.header().remaining(), is(0));

        CodecBuffer footer = sut.footer();
        byte[] footerContent = new byte[4];
        footer.readBytes(footerContent, 0, footerContent.length);
        assertThat(footerContent, is(data));
        assertThat(sut.footer().remaining(), is(0));
    }

    @Test
    public void testAddLast_Two() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 10);
        byte[] data0 = new byte[2];
        Arrays.fill(data0, (byte) 1);
        CodecBuffer added0 = Buffers.wrap(data0, 0, data0.length);
        byte[] data1 = new byte[2];
        Arrays.fill(data1, (byte) -1);
        CodecBuffer added1 = Buffers.wrap(data1, 0, data1.length);

        sut.addLast(added0);
        sut.addLast(added1);

        assertThat(sut.header().remaining(), is(0));
        byte[] footerContent = new byte[4];
        sut.footer().readBytes(footerContent, 0, footerContent.length);
        assertThat(footerContent, is(new byte[]{1, 1, -1, -1}));
        assertThat(sut.footer().remaining(), is(0));
    }

    @Test
    public void testSlice_ReferenceCount() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 10);
        ByteBufferChunkPool pool = new ByteBufferChunkPool(10, true);
        closeableList.add(pool);
        ByteBufferCodecBuffer b0 = (ByteBufferCodecBuffer) Buffers.newCodecBuffer(pool, 5);
        b0.writeBytes(new byte[5], 0, 5);
        ByteBufferCodecBuffer b1 = (ByteBufferCodecBuffer) Buffers.newCodecBuffer(pool, 5);
        b1.writeBytes(new byte[5], 0, 5);
        sut.addFirst(b0);
        sut.addLast(b1);

        // first and second
        Packet sliced0 = sut.slice(10);
        assertThat(sut.referenceCount(), is(2));
        assertThat(b0.chunk().referenceCount(), is(2));
        assertThat(b1.chunk().referenceCount(), is(1));

        // second and third
        Packet sliced1 = sut.slice(10);
        assertThat(sut.referenceCount(), is(3));
        assertThat(b0.chunk().referenceCount(), is(2));
        assertThat(b1.chunk().referenceCount(), is(2));

        sliced0.dispose();
        assertThat(sut.referenceCount(), is(2));
        assertThat(b0.chunk().referenceCount(), is(1));
        assertThat(b1.chunk().referenceCount(), is(2));

        sliced1.dispose();
        assertThat(sut.referenceCount(), is(1));
        assertThat(b0.chunk().referenceCount(), is(1));
        assertThat(b1.chunk().referenceCount(), is(1));

        sut.dispose();
        assertThat(sut.referenceCount(), is(0));
        assertThat(b0.chunk().referenceCount(), is(0));
        assertThat(b1.chunk().referenceCount(), is(0));
    }

    @Test
    public void testDuplicate_ReferenceCount() throws Exception {
        FilePacket sut = new FilePacket(channel_, 0, 10);
        ByteBufferChunkPool pool = new ByteBufferChunkPool(10, true);
        closeableList.add(pool);
        ByteBufferCodecBuffer b0 = (ByteBufferCodecBuffer) Buffers.newCodecBuffer(pool, 5);
        b0.writeBytes(new byte[5], 0, 5);
        ByteBufferCodecBuffer b1 = (ByteBufferCodecBuffer) Buffers.newCodecBuffer(pool, 5);
        b1.writeBytes(new byte[5], 0, 5);
        sut.addFirst(b0);
        sut.addLast(b1);

        Packet d0 = sut.duplicate();
        assertThat(sut.referenceCount(), is(2));
        assertThat(b0.chunk().referenceCount(), is(2));
        assertThat(b1.chunk().referenceCount(), is(2));

        Packet d1 = sut.duplicate();
        assertThat(sut.referenceCount(), is(3));
        assertThat(b0.chunk().referenceCount(), is(3));
        assertThat(b1.chunk().referenceCount(), is(3));

        d0.dispose();
        assertThat(sut.referenceCount(), is(2));
        assertThat(b0.chunk().referenceCount(), is(2));
        assertThat(b1.chunk().referenceCount(), is(2));

        d1.dispose();
        assertThat(sut.referenceCount(), is(1));
        assertThat(b0.chunk().referenceCount(), is(1));
        assertThat(b1.chunk().referenceCount(), is(1));

        sut.dispose();
        assertThat(sut.referenceCount(), is(0));
        assertThat(b0.chunk().referenceCount(), is(0));
        assertThat(b1.chunk().referenceCount(), is(0));
    }

    @Test
    public void testEquals() throws Exception {
        GatheringByteChannel outputChannel = mock(GatheringByteChannel.class);
        when(outputChannel.write(Mockito.any(ByteBuffer.class))).thenAnswer(new TransferAll());
        when(outputChannel.isOpen()).thenReturn(true);

        byte[] header = new byte[1];
        byte[] footer = new byte[2];
        FilePacket sut = new FilePacket(channel_, 0, originalData_.length);
        sut.addFirst(Buffers.wrap(header));
        sut.addLast(Buffers.wrap(footer));
        Packet obj = Buffers.wrap(Buffers.wrap(header), Buffers.wrap(originalData_), Buffers.wrap(footer));
        assertThat(sut.equals(obj), is(true));
        sut.dispose();
    }
}
