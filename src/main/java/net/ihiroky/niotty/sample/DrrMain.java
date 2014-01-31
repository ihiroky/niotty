package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Pipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StageKey;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import net.ihiroky.niotty.codec.DeficitRoundRobinEncoder;
import net.ihiroky.niotty.codec.LengthFrameCodec;
import net.ihiroky.niotty.nio.NioClientSocketProcessor;
import net.ihiroky.niotty.nio.NioClientSocketTransport;
import net.ihiroky.niotty.nio.NioServerSocketProcessor;
import net.ihiroky.niotty.nio.NioServerSocketTransport;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * A sample using {@link net.ihiroky.niotty.codec.DeficitRoundRobinEncoder}.
 */
public class DrrMain {

    public static void main(String[] args) {
        new DrrMain().execute(args);
    }

    private enum MyStageKey implements StageKey {
        GENERATOR,
        DEFICIT_ROUND_ROBIN,
        REPORTER,
        FRAMING,
    }

    private void execute(String[] args) {
        final int serverPort = 10000;

        NioServerSocketProcessor serverProcessor = new NioServerSocketProcessor()
                .setPipelineComposer(new PipelineComposer() {
                    @Override
                    public void compose(Pipeline pipeline) {
                        pipeline.add(MyStageKey.GENERATOR, new NumberGenerator())
                                .add(MyStageKey.DEFICIT_ROUND_ROBIN,
                                        new DeficitRoundRobinEncoder(1024, 1, TimeUnit.MILLISECONDS, 0.5f))
                                .add(MyStageKey.FRAMING, new LengthFrameCodec());
                    }
                });

        NioClientSocketProcessor clientProcessor = new NioClientSocketProcessor();
        clientProcessor.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(Pipeline pipeline) {
                pipeline.add(MyStageKey.REPORTER, new EvenOddReporter())
                        .add(MyStageKey.FRAMING, new LengthFrameCodec());
            }
        });
        serverProcessor.start();
        clientProcessor.start();

        NioServerSocketTransport serverTransport = serverProcessor.createTransport();
        NioClientSocketTransport clientTransport = clientProcessor.createTransport();
        try {
            InetSocketAddress endpoint = new InetSocketAddress(serverPort);
            serverTransport.bind(endpoint)
                    .await()
                    .throwExceptionIfFailed();
            clientTransport.connect(endpoint)
                    .await()
                    .throwExceptionIfFailed();

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
