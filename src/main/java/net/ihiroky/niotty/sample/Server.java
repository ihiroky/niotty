package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Pipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StageKeys;
import net.ihiroky.niotty.codec.DelimiterCodec;
import net.ihiroky.niotty.codec.StringCodec;
import net.ihiroky.niotty.nio.NioServerSocketProcessor;
import net.ihiroky.niotty.nio.NioServerSocketTransport;

import java.net.InetSocketAddress;

/**
 * @author Hiroki Itoh
 */
public class Server {

    public static void main(String[] args) {
        final int serverPort = 10000;

        NioServerSocketProcessor processor = new NioServerSocketProcessor();
        processor.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(Pipeline pipeline) {
                pipeline.add(StageKeys.of("load-app"), new Dump(true))
                        .add(StageKeys.of("load-string"), new StringCodec())
                .add(StageKeys.of("load-frame"), new DelimiterCodec(new byte[]{'\n'}, true));
            }
        });
        processor.start();
        final NioServerSocketTransport transport = processor.createTransport();
        transport.bind(new InetSocketAddress(serverPort));

        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            transport.close();
            processor.stop();
        }

    }

}
