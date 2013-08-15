package net.ihiroky.niotty.sample.echo;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.codec.FrameLengthPrependEncoder;
import net.ihiroky.niotty.codec.FrameLengthRemoveDecoder;
import net.ihiroky.niotty.codec.StringDecoder;
import net.ihiroky.niotty.codec.StringEncoder;
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
            public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(EchoStageKey.LOAD_FRAMING, new FrameLengthRemoveDecoder())
                        .add(EchoStageKey.LOAD_DECODE, new StringDecoder())
                        .add(EchoStageKey.LOAD_APPLICATION, new EchoStage());
                storePipeline.add(EchoStageKey.STORE_ENCODE, new StringEncoder())
                        .add(EchoStageKey.STORE_FRAMING, new FrameLengthPrependEncoder());
            }
        });
        NioClientSocketProcessor client = new NioClientSocketProcessor();
        client.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(EchoStageKey.LOAD_FRAMING, new FrameLengthRemoveDecoder())
                        .add(EchoStageKey.LOAD_DECODE, new StringDecoder())
                        .add(EchoStageKey.LOAD_APPLICATION, new HelloWorldStage());
                storePipeline.add(EchoStageKey.STORE_ENCODE, new StringEncoder())
                        .add(EchoStageKey.STORE_FRAMING, new FrameLengthPrependEncoder());
            }
        });

        server.start();
        client.start();

        final NioServerSocketTransport serverTransport = server.createTransport();
        NioClientSocketTransport clientTransport = client.createTransport();

        try {
            serverTransport.bind(new InetSocketAddress(port));

            clientTransport.connect(new InetSocketAddress("localhost", port)).waitForCompletion();
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
