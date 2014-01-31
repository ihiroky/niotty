package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Pipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.codec.LengthFrameCodec;
import net.ihiroky.niotty.codec.StringCodec;
import net.ihiroky.niotty.nio.NioClientSocketProcessor;
import net.ihiroky.niotty.nio.NioClientSocketTransport;
import net.ihiroky.niotty.nio.NioServerSocketProcessor;
import net.ihiroky.niotty.nio.NioServerSocketTransport;

import java.net.InetSocketAddress;

/**
 * Created on 13/01/18, 12:59
 *
 * @author Hiroki Itoh
 */
public class EchoMain {

    public static void main(String[] args) {
        new EchoMain().execute(args);
    }

    private void execute(String[] args) {
        final int port = 10000;
        final int lastWaitMillis = 500;

        NioServerSocketProcessor server = new NioServerSocketProcessor();
        server.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(Pipeline pipeline) {
                pipeline.add(EchoStageKey.APPLICATION, new EchoStage())
                        .add(EchoStageKey.STRING, new StringCodec())
                        .add(EchoStageKey.FRAMING, new LengthFrameCodec());
            }
        });
        NioClientSocketProcessor client = new NioClientSocketProcessor();
        client.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(Pipeline pipeline) {
                pipeline.add(EchoStageKey.APPLICATION, new HelloWorldStage())
                        .add(EchoStageKey.STRING, new StringCodec())
                        .add(EchoStageKey.FRAMING, new LengthFrameCodec());
            }
        });

        server.start();
        client.start();

        final NioServerSocketTransport serverTransport = server.createTransport();
        NioClientSocketTransport clientTransport = client.createTransport();

        try {
            serverTransport.bind(new InetSocketAddress(port))
                    .await().throwExceptionIfFailed();

            clientTransport.connect(new InetSocketAddress("localhost", port))
                    .await().throwExceptionIfFailed();

            System.out.println("connection wait gets done.");
            clientTransport.write("Hello World.");

            Thread.sleep(lastWaitMillis);
            System.out.println("end.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clientTransport.close();
            serverTransport.close();
            client.stop();
            server.stop();
        }
    }
}
