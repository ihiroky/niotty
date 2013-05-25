package net.ihiroky.niotty.sample.echo;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportListener;
import net.ihiroky.niotty.codec.StringDecoder;
import net.ihiroky.niotty.codec.StringEncoder;
import net.ihiroky.niotty.nio.NioDatagramSocketConfig;
import net.ihiroky.niotty.nio.NioDatagramSocketProcessor;

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
        final Transport serverTransport = server.createTransport(new NioDatagramSocketConfig());
        serverTransport.addListener(new TransportListener() {
            @Override
            public void onConnect(Transport transport, SocketAddress remoteAddress) {
                serverTransport.write("new Transport " + transport + " is accepted on " + Thread.currentThread());
            }

            @Override
            public void onClose(Transport transport) {
            }
        });

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
        Transport clientTransport = client.createTransport(new NioDatagramSocketConfig());

        try {
            serverTransport.bind(new InetSocketAddress("localhost", serverPort));
            clientTransport.bind(new InetSocketAddress("localhost", clientPort));
            serverTransport.connect(new InetSocketAddress("localhost", clientPort));
            clientTransport.connect(new InetSocketAddress("localhost", serverPort));
            clientTransport.write("Hello World.");

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
