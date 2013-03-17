package net.ihiroky.niotty.sample;

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
import net.ihiroky.niotty.stage.codec.frame.FrameLengthPrependEncoder;
import net.ihiroky.niotty.stage.codec.frame.FrameLengthRemoveDecoder;

import java.net.InetSocketAddress;

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
        Transport serverTransport = null;
        Transport clientTransport = null;
        try {
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
            serverTransport = server.createTransport(serverConfig);
            serverTransport.bind(new InetSocketAddress(port));

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
            clientTransport = client.createTransport(clientConfig);
            TransportFuture connectFuture = clientTransport.connect(new InetSocketAddress("localhost", port));
            connectFuture.waitForCompletion();
            System.out.println("connection wait gets done.");
            serverTransport.write("broadcast from server on thread " + Thread.currentThread());

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
