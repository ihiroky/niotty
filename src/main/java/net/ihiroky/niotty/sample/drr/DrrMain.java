package net.ihiroky.niotty.sample.drr;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StageKey;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.codec.FrameLengthPrependEncoder;
import net.ihiroky.niotty.codec.FrameLengthRemoveDecoder;
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

    private enum MyStageKey implements StageKey {
        GENERATOR,
        REPORTER,
        FRAMING,
    }

    private void execute(String[] args) {
        final int serverPort = 10000;

        NioServerSocketProcessor serverProcessor = new NioServerSocketProcessor();
        serverProcessor.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(MyStageKey.FRAMING, new FrameLengthRemoveDecoder())
                        .add(MyStageKey.GENERATOR, new NumberGenerator());
                storePipeline.add(MyStageKey.FRAMING, new FrameLengthPrependEncoder());
            }
        });
        NioClientSocketProcessor clientProcessor = new NioClientSocketProcessor();
        clientProcessor.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(MyStageKey.FRAMING, new FrameLengthRemoveDecoder())
                        .add(MyStageKey.REPORTER, new EvenOddReporter());
                storePipeline.add(MyStageKey.FRAMING, new FrameLengthPrependEncoder());
            }
        });
        serverProcessor.start();
        clientProcessor.start();
        Transport serverTransport = serverProcessor.createTransport(new NioServerSocketConfig());
        Transport clientTransport = clientProcessor.createTransport(new NioClientSocketConfig());
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
}
