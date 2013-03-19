package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineInitializer;
import net.ihiroky.niotty.StageKey;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportListener;
import net.ihiroky.niotty.nio.NioClientSocketConfig;
import net.ihiroky.niotty.nio.NioClientSocketProcessor;
import net.ihiroky.niotty.nio.NioServerSocketConfig;
import net.ihiroky.niotty.nio.NioServerSocketProcessor;
import net.ihiroky.niotty.stage.codec.frame.FrameLengthPrependEncoder;
import net.ihiroky.niotty.stage.codec.frame.FrameLengthRemoveDecoder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;

/**
 * Created on 13/01/18, 12:59
 *
 * @author Hiroki Itoh
 */
public class Main {

    private enum MyStageKey implements StageKey {
        LOAD_FRAMING,
        LOAD_DECODE,
        LOAD_APPLICATION,
        STORE_ENCODE,
        STORE_FRAMING,
    }

    public static void main(String[] args) {

        final int port = 10000;
        final int lastWaitMillis = 500;

        NioServerSocketProcessor server = new NioServerSocketProcessor();
        NioClientSocketProcessor client = new NioClientSocketProcessor();
        server.start();
        client.start();
        NioServerSocketConfig serverConfig = new NioServerSocketConfig();
        serverConfig.setPipelineInitializer(new PipelineInitializer() {
            @Override
            public void setUpPipeline(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(MyStageKey.LOAD_FRAMING, new FrameLengthRemoveDecoder())
                        .add(MyStageKey.LOAD_DECODE, new StringDecoder())
                        .add(MyStageKey.LOAD_APPLICATION, new EchoStage());
                storePipeline.add(MyStageKey.STORE_ENCODE, new StringEncoder())
                        .add(MyStageKey.STORE_FRAMING, new FrameLengthPrependEncoder());
            }
        });
        final Transport serverTransport = server.createTransport(serverConfig);
        serverTransport.addListener(new TransportListener() {
            @Override
            public void onBind(Transport transport, SocketAddress localAddress) {
            }

            @Override
            public void onConnect(Transport transport, SocketAddress remoteAddress) {
                serverTransport.write("new Transport " + transport + " is accepted on " + Thread.currentThread());
            }

            @Override
            public void onJoin(Transport transport, InetAddress group, NetworkInterface networkInterface, InetAddress source) {
            }

            @Override
            public void onClose(Transport transport) {
            }
        });
        NioClientSocketConfig clientConfig = new NioClientSocketConfig();
        clientConfig.setPipelineInitializer(new PipelineInitializer() {
            @Override
            public void setUpPipeline(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(MyStageKey.LOAD_FRAMING, new FrameLengthRemoveDecoder())
                        .add(MyStageKey.LOAD_DECODE, new StringDecoder())
                        .add(MyStageKey.LOAD_APPLICATION, new HelloWorldStage());
                storePipeline.add(MyStageKey.STORE_ENCODE, new StringEncoder())
                        .add(MyStageKey.STORE_FRAMING, new FrameLengthPrependEncoder());
            }
        });
        final Transport clientTransport = client.createTransport(clientConfig);
        try {
            serverTransport.bind(new InetSocketAddress(port));

            TransportFuture connectFuture = clientTransport.connect(new InetSocketAddress("localhost", port));
            connectFuture.waitForCompletion();
            System.out.println("connection wait gets done.");

            Thread.sleep(lastWaitMillis);
            System.out.println("end.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (clientTransport != null) {
                clientTransport.close();
            }
            if (serverTransport != null) {
                serverTransport.close();
            }
            client.stop();
            server.stop();
        }
    }
}
