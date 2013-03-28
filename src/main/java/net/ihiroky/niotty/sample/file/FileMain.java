package net.ihiroky.niotty.sample.file;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineInitializer;
import net.ihiroky.niotty.StageKey;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.nio.NioClientSocketConfig;
import net.ihiroky.niotty.nio.NioClientSocketProcessor;
import net.ihiroky.niotty.nio.NioServerSocketConfig;
import net.ihiroky.niotty.nio.NioServerSocketProcessor;
import net.ihiroky.niotty.sample.StringDecoder;
import net.ihiroky.niotty.sample.StringEncoder;
import net.ihiroky.niotty.stage.codec.frame.FrameLengthPrependEncoder;
import net.ihiroky.niotty.stage.codec.frame.FrameLengthRemoveDecoder;

import java.net.InetSocketAddress;

/**
 * @author Hiroki Itoh
 */
public class FileMain {

    public static void main(String[] args) {
        new FileMain().execute(args);
    }

    private void execute(String[] args) {
        int serverPort = 10000;

        NioServerSocketProcessor serverProcessor = new NioServerSocketProcessor();
        NioClientSocketProcessor clientProcessor = new NioClientSocketProcessor();
        serverProcessor.start();
        clientProcessor.start();

        Waiter waiter = new Waiter();
        Transport serverTransport = serverProcessor.createTransport(createServerConfig());
        Transport clientTransport = clientProcessor.createTransport(createClientConfig(waiter));
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

    private enum Key implements StageKey {
        LOAD_FILE,
        STRING_DECODER,
        STRING_ENCODER,
        FRAMING,
        DUMP_FILE,
    }

    private NioServerSocketConfig createServerConfig() {
        NioServerSocketConfig config = new NioServerSocketConfig();
        config.setPipelineInitializer(new PipelineInitializer() {
            @Override
            public void setUpPipeline(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(Key.FRAMING, new FrameLengthRemoveDecoder())
                        .add(Key.STRING_DECODER, new StringDecoder())
                        .add(Key.LOAD_FILE, new FileLoadStage());
                storePipeline.add(Key.FRAMING, new FrameLengthPrependEncoder());
            }
        });
        return config;
    }

    private NioClientSocketConfig createClientConfig(final Waiter waiter) {
        NioClientSocketConfig config = new NioClientSocketConfig();
        config.setPipelineInitializer(new PipelineInitializer() {
            @Override
            public void setUpPipeline(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(Key.FRAMING, new FrameLengthRemoveDecoder())
                        .add(Key.DUMP_FILE, new FileDumpStage(waiter));
                storePipeline.add(Key.STRING_ENCODER, new StringEncoder())
                        .add(Key.FRAMING, new FrameLengthPrependEncoder());
            }
        });
        return config;
    }
}
