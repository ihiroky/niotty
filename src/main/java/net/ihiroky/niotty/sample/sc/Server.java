package net.ihiroky.niotty.sample.sc;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StageKeys;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.codec.FrameLengthPrependEncoder;
import net.ihiroky.niotty.codec.FrameLengthRemoveDecoder;
import net.ihiroky.niotty.codec.StringDecoder;
import net.ihiroky.niotty.codec.StringEncoder;
import net.ihiroky.niotty.nio.NioServerSocketConfig;
import net.ihiroky.niotty.nio.NioServerSocketProcessor;
import net.ihiroky.niotty.nio.NioServerSocketTransport;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Hiroki Itoh
 */
public class Server {

    public static void main(String[] args) {
        final int serverPort = 10000;

        NioServerSocketProcessor processor = new NioServerSocketProcessor();
        processor.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(StageKeys.of("load-frame"), new FrameLengthRemoveDecoder())
                        .add(StageKeys.of("load-string"), new StringDecoder())
                        .add(StageKeys.of("load-app"), new Dump());
                storePipeline.add(StageKeys.of("store-string"), new StringEncoder())
                        .add(StageKeys.of("store-frame"), new FrameLengthPrependEncoder());
            }
        });
        processor.start();
        final NioServerSocketTransport transport = processor.createTransport(new NioServerSocketConfig());
        transport.bind(new InetSocketAddress(serverPort));

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                transport.write(new Date().toString());
            }
        }, 1, 1, TimeUnit.SECONDS);
        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
            transport.close();
            processor.stop();
        }

    }

}
