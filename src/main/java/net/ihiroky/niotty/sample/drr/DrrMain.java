package net.ihiroky.niotty.sample.drr;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineInitializer;
import net.ihiroky.niotty.StageKey;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.codec.FrameLengthPrependEncoder;
import net.ihiroky.niotty.codec.FrameLengthRemoveDecoder;
import net.ihiroky.niotty.nio.DeficitRoundRobinWriteQueueFactory;
import net.ihiroky.niotty.nio.NioClientSocketConfig;
import net.ihiroky.niotty.nio.NioClientSocketProcessor;
import net.ihiroky.niotty.nio.NioServerSocketConfig;
import net.ihiroky.niotty.nio.NioServerSocketProcessor;

import java.net.InetSocketAddress;

/**
 * @author Hiroki Itoh
 */
public class DrrMain {

    public static void main(String[] args) {
        new DrrMain().execute(args);
    }

    private void execute(String[] args) {
        final int serverPort = 10000;

        NioServerSocketProcessor serverProcessor = new NioServerSocketProcessor();
        NioClientSocketProcessor clientProcessor = new NioClientSocketProcessor();
        serverProcessor.start();
        clientProcessor.start();
        Transport serverTransport = createServerTransport(serverProcessor);
        Transport clientTransport = createClientTransport(clientProcessor);
        try {
            InetSocketAddress endpoint = new InetSocketAddress(serverPort);
            serverTransport.bind(endpoint);
            TransportFuture connectFuture = clientTransport.connect(endpoint);
            connectFuture.waitForCompletion();

            CodecBuffer buffer = Buffers.newCodecBuffer(4);
            buffer.writeInt(30);
            clientTransport.write(buffer);
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clientTransport.close();
            serverTransport.close();
            clientProcessor.stop();
            serverProcessor.stop();
        }
    }

    private enum MyStageKey implements StageKey {
        GENERATOR,
        REPORTER,
        FRAMING,
    }

    private Transport createServerTransport(NioServerSocketProcessor processor) {
        NioServerSocketConfig config = new NioServerSocketConfig();
        config.setPipelineInitializer(new PipelineInitializer() {
            @Override
            public void setUpPipeline(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(MyStageKey.FRAMING, new FrameLengthRemoveDecoder())
                        .add(MyStageKey.GENERATOR, new NumberGenerator());
                storePipeline.add(MyStageKey.FRAMING, new FrameLengthPrependEncoder());
            }
        });
        config.setWriteQueueFactory(new DeficitRoundRobinWriteQueueFactory(4096, 0.5f));
        return processor.createTransport(config);
    }

    private Transport createClientTransport(NioClientSocketProcessor processor) {
        NioClientSocketConfig config = new NioClientSocketConfig();
        config.setPipelineInitializer(new PipelineInitializer() {
            @Override
            public void setUpPipeline(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(MyStageKey.FRAMING, new FrameLengthRemoveDecoder())
                        .add(MyStageKey.REPORTER, new EvenOddReporter());
                storePipeline.add(MyStageKey.FRAMING, new FrameLengthPrependEncoder());
            }
        });
        return processor.createTransport(config);
    }
}
