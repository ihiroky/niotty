package net.ihiroky.niotty.sample.echo;

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
import net.ihiroky.niotty.sample.StringDecoder;
import net.ihiroky.niotty.sample.StringEncoder;
import net.ihiroky.niotty.stage.codec.frame.FrameLengthPrependEncoder;
import net.ihiroky.niotty.stage.codec.frame.FrameLengthRemoveDecoder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Created on 13/01/18, 12:59
 *
 * @author Hiroki Itoh
 */
public class EchoMain {

    private enum MyStageKey implements StageKey {
        LOAD_FRAMING,
        LOAD_DECODE,
        LOAD_APPLICATION,
        STORE_ENCODE,
        STORE_FRAMING,
    }

    public static void main(String[] args) {
        new EchoMain().execute(args);
    }

    private void execute(String[] args) {
        final int port = 10000;
        final int lastWaitMillis = 500;

        NioServerSocketProcessor server = new NioServerSocketProcessor();
        NioClientSocketProcessor client = new NioClientSocketProcessor();
        server.start();
        client.start();

        Transport serverTransport = createServerTransport(server);
        Transport clientTransport = createClientTransport(client);

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
            clientTransport.close();
            serverTransport.close();
            client.stop();
            server.stop();
        }
    }

    private Transport createServerTransport(NioServerSocketProcessor server) {
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
            public void onConnect(Transport transport, SocketAddress remoteAddress) {
                serverTransport.write("new Transport " + transport + " is accepted on " + Thread.currentThread());
            }
            @Override
            public void onClose(Transport transport) {
            }
        });

        return serverTransport;
    }

    private Transport createClientTransport(NioClientSocketProcessor client) {
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
        return client.createTransport(clientConfig);
    }
}
