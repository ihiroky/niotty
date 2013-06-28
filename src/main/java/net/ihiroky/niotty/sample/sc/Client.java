package net.ihiroky.niotty.sample.sc;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StageKeys;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportFutureListener;
import net.ihiroky.niotty.codec.DelimiterDecoder;
import net.ihiroky.niotty.codec.DelimiterEncoder;
import net.ihiroky.niotty.codec.StringDecoder;
import net.ihiroky.niotty.codec.StringEncoder;
import net.ihiroky.niotty.nio.NioClientSocketConfig;
import net.ihiroky.niotty.nio.NioClientSocketProcessor;
import net.ihiroky.niotty.nio.NioClientSocketTransport;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Hiroki Itoh
 */
public class Client {

    public static void main(String[] args) {
        final int serverPort = 10000;

        NioClientSocketProcessor processor = new NioClientSocketProcessor();
        processor.setPipelineComposer(new PipelineComposer() {
            @Override
            public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                loadPipeline.add(StageKeys.of("load-frame"), new DelimiterDecoder(new byte[]{'\n'}, true))
                        .add(StageKeys.of("load-string"), new StringDecoder())
                        .add(StageKeys.of("load-app"), new Dump());
                storePipeline.add(StageKeys.of("store-string"), new StringEncoder())
                        .add(StageKeys.of("store-frame"), new DelimiterEncoder(new byte[]{'\n'}));
            }
        });
        processor.start();
        final NioClientSocketTransport transport = processor.createTransport(new NioClientSocketConfig());
        transport.connect(new InetSocketAddress(serverPort));

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        final Future<?> scheduledFuture = executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                transport.write(new Date().toString());
            }
        }, 1, 1, TimeUnit.SECONDS);
        transport.closeFuture().addListener(new TransportFutureListener() {
            @Override
            public void onComplete(TransportFuture future) {
                System.out.println("Cancel the scheduler.");
                scheduledFuture.cancel(true);
            }
        });

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
