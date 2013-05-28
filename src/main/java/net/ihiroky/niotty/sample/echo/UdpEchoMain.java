package net.ihiroky.niotty.sample.echo;

import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.codec.StringDecoder;
import net.ihiroky.niotty.codec.StringEncoder;
import net.ihiroky.niotty.nio.NioDatagramSocketConfig;
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
            public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(EchoStageKey.LOAD_DECODE, new StringDecoder())
                        .add(EchoStageKey.LOAD_APPLICATION, new EchoStage());
                storePipeline.add(EchoStageKey.STORE_ENCODE, new StringEncoder());
            }
        });
        server.start();
        NioDatagramSocketTransport serverTransport = server.createTransport(new NioDatagramSocketConfig());

        NioDatagramSocketProcessor client = new NioDatagramSocketProcessor();
        client.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(EchoStageKey.LOAD_DECODE, new StringDecoder())
                        .add(EchoStageKey.LOAD_APPLICATION, new HelloWorldStage());
                storePipeline.add(EchoStageKey.STORE_ENCODE, new StringEncoder());
            }
        });
        client.start();
        NioDatagramSocketTransport clientTransport = client.createTransport(new NioDatagramSocketConfig());

        try {
            SocketAddress serverEndpoint = new InetSocketAddress("localhost", serverPort);
            SocketAddress clientEndPoint = new InetSocketAddress("localhost", clientPort);
            serverTransport.bind(serverEndpoint);
            clientTransport.bind(clientEndPoint);
            serverTransport.connect(clientEndPoint);
            clientTransport.connect(serverEndpoint);
            clientTransport.write("Hello World.");

            Thread.sleep(lastWaitMillis);

            clientTransport.disconnect();
            clientTransport.write("Hello after disconnected.", new DefaultTransportParameter(serverEndpoint));

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
