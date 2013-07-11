package net.ihiroky.niotty.sample.file;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StageKey;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.codec.FrameLengthPrependEncoder;
import net.ihiroky.niotty.codec.FrameLengthRemoveDecoder;
import net.ihiroky.niotty.codec.StringDecoder;
import net.ihiroky.niotty.codec.StringEncoder;
import net.ihiroky.niotty.nio.NioClientSocketConfig;
import net.ihiroky.niotty.nio.NioClientSocketProcessor;
import net.ihiroky.niotty.nio.NioClientSocketTransport;
import net.ihiroky.niotty.nio.NioServerSocketConfig;
import net.ihiroky.niotty.nio.NioServerSocketProcessor;
import net.ihiroky.niotty.nio.NioServerSocketTransport;

import java.net.InetSocketAddress;

/**
 * @author Hiroki Itoh
 */
public class FileMain {

    public static void main(String[] args) {
        new FileMain().execute(args);
    }

    private enum Key implements StageKey {
        LOAD_FILE,
        STRING_DECODER,
        STRING_ENCODER,
        FRAMING,
        DUMP_FILE,
    }

    private void execute(String[] args) {
        int serverPort = 10000;

        final Waiter waiter = new Waiter();
        NioServerSocketProcessor serverProcessor = new NioServerSocketProcessor();
        serverProcessor.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(Key.FRAMING, new FrameLengthRemoveDecoder())
                        .add(Key.STRING_DECODER, new StringDecoder())
                        .add(Key.LOAD_FILE, new FileLoadStage());
                storePipeline.add(Key.FRAMING, new FrameLengthPrependEncoder());
            }
        });
        NioClientSocketProcessor clientProcessor = new NioClientSocketProcessor();
        clientProcessor.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(Key.FRAMING, new FrameLengthRemoveDecoder())
                        .add(Key.DUMP_FILE, new FileDumpStage(waiter));
                storePipeline.add(Key.STRING_ENCODER, new StringEncoder())
                        .add(Key.FRAMING, new FrameLengthPrependEncoder());
            }
        });
        serverProcessor.start();
        clientProcessor.start();

        NioServerSocketTransport serverTransport = serverProcessor.createTransport(new NioServerSocketConfig());
        NioClientSocketTransport clientTransport = clientProcessor.createTransport(new NioClientSocketConfig());
        try {
            serverTransport.bind(new InetSocketAddress(serverPort));
            TransportFuture connectFuture = clientTransport.connect(new InetSocketAddress("localhost", serverPort));
            connectFuture.waitForCompletion();

            String path = "build.gradle";
            clientTransport.write(path);
            waiter.waitUntilFinished();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clientTransport.close();
            serverTransport.close();
            clientProcessor.stop();
            serverProcessor.stop();
        }
    }
}
