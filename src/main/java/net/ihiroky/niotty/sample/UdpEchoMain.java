package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Pipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.codec.StringCodec;
import net.ihiroky.niotty.nio.NioDatagramSocketProcessor;
import net.ihiroky.niotty.nio.NioDatagramSocketTransport;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author Hiroki Itoh
 */
public class UdpEchoMain {

    public static void main(String[] args) {
        final int serverPort = 10000;
        final int clientPort = 10001;
        final int lastWaitMillis = 500;

        NioDatagramSocketProcessor server = new NioDatagramSocketProcessor();
        server.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(Pipeline pipeline) {
                pipeline.add(EchoStageKey.APPLICATION, new EchoStage())
                        .add(EchoStageKey.STRING, new StringCodec());
            }
        });
        server.start();
        NioDatagramSocketTransport serverTransport = server.createTransport();

        NioDatagramSocketProcessor client = new NioDatagramSocketProcessor();
        client.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(Pipeline pipeline) {
                pipeline.add(EchoStageKey.APPLICATION, new HelloWorldStage())
                        .add(EchoStageKey.STRING, new StringCodec());
            }
        });
        client.start();
        NioDatagramSocketTransport clientTransport = client.createTransport();

        try {
            SocketAddress serverEndpoint = new InetSocketAddress("localhost", serverPort);
            SocketAddress clientEndPoint = new InetSocketAddress("localhost", clientPort);
            serverTransport.bind(serverEndpoint).await().throwExceptionIfFailed();
            clientTransport.bind(clientEndPoint).await().throwExceptionIfFailed();
            serverTransport.connect(clientEndPoint).await().throwExceptionIfFailed();
            clientTransport.connect(serverEndpoint).await().throwExceptionIfFailed();
            clientTransport.write("Hello World 0.");
            clientTransport.write("Hello World 1.");

            Thread.sleep(lastWaitMillis);

            clientTransport.disconnect();
            clientTransport.write("Hello after disconnected.", serverEndpoint);

            Thread.sleep(lastWaitMillis);

            System.out.println("end.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clientTransport.close();
            serverTransport.close();
            server.stop();
            client.stop();
        }
    }
}
