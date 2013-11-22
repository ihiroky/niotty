package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Pipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StageKey;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.codec.FramingCodec;
import net.ihiroky.niotty.nio.NioClientSocketProcessor;
import net.ihiroky.niotty.nio.NioClientSocketTransport;
import net.ihiroky.niotty.nio.NioServerSocketProcessor;
import net.ihiroky.niotty.nio.NioServerSocketTransport;

import java.net.InetSocketAddress;

/**
 * File transmission sample. The system property user.dir must be the directory in which "build.gradle" exists
 */
public class FileMain {

    public static void main(String[] args) {
        new FileMain().execute(args);
    }

    private enum Key implements StageKey {
        LOAD_FILE,
        FRAMING,
        DUMP_FILE,
    }

    private void execute(String[] args) {
        int serverPort = 10000;

        final Waiter waiter = new Waiter();
        NioServerSocketProcessor serverProcessor = new NioServerSocketProcessor();
        serverProcessor.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(Pipeline pipeline) {
                pipeline.add(Key.LOAD_FILE, new FileLoadStage())
                        .add(Key.FRAMING, new FramingCodec());
            }
        });
        NioClientSocketProcessor clientProcessor = new NioClientSocketProcessor();
        clientProcessor.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(Pipeline pipeline) {
                pipeline.add(Key.DUMP_FILE, new FileDumpStage(waiter))
                        .add(Key.FRAMING, new FramingCodec());
            }
        });
        serverProcessor.start();
        clientProcessor.start();

        NioServerSocketTransport serverTransport = serverProcessor.createTransport();
        NioClientSocketTransport clientTransport = clientProcessor.createTransport();
        try {
            serverTransport.bind(new InetSocketAddress(serverPort)).await();
            TransportFuture connectFuture = clientTransport.connect(new InetSocketAddress("localhost", serverPort));
            connectFuture.await().throwExceptionIfFailed();

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
