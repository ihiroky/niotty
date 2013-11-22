package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.CompletionListener;
import net.ihiroky.niotty.Pipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StageKeys;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.codec.DelimiterCodec;
import net.ihiroky.niotty.codec.StringCodec;
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
            public void compose(Pipeline pipeline) {
                pipeline.add(StageKeys.of("load-app"), new Dump())
                        .add(StageKeys.of("load-string"), new StringCodec())
                        .add(StageKeys.of("load-frame"), new DelimiterCodec(new byte[]{'\n'}, true));
            }
        });
        processor.start();
        final NioClientSocketTransport transport = processor.createTransport();
        transport.connect(new InetSocketAddress(serverPort));

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        final Future<?> scheduledFuture = executor.scheduleAtFixedRate(new Runnable() {
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
            transport.close().addListener(new CompletionListener() {
                @Override
                public void onComplete(TransportFuture future) {
                    System.out.println("Cancel the scheduler.");
                    scheduledFuture.cancel(true);
                }
            });
            processor.stop();
        }

    }

}
