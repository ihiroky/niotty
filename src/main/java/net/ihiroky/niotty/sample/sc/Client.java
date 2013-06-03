package net.ihiroky.niotty.sample.sc;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StageKeys;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportListener;
import net.ihiroky.niotty.codec.FrameLengthPrependEncoder;
import net.ihiroky.niotty.codec.FrameLengthRemoveDecoder;
import net.ihiroky.niotty.codec.StringDecoder;
import net.ihiroky.niotty.codec.StringEncoder;
import net.ihiroky.niotty.nio.NioClientSocketConfig;
import net.ihiroky.niotty.nio.NioClientSocketProcessor;
import net.ihiroky.niotty.nio.NioClientSocketTransport;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
                loadPipeline.add(StageKeys.of("load-frame"), new FrameLengthRemoveDecoder())
                        .add(StageKeys.of("load-string"), new StringDecoder())
                        .add(StageKeys.of("load-app"), new Dump());
                storePipeline.add(StageKeys.of("store-string"), new StringEncoder())
                        .add(StageKeys.of("store-frame"), new FrameLengthPrependEncoder());
            }
        });
        processor.start();
        final NioClientSocketTransport transport = processor.createTransport(new NioClientSocketConfig());
        transport.connect(new InetSocketAddress(serverPort));

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        final Future future = executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                transport.write(new Date().toString());
            }
        }, 1, 1, TimeUnit.SECONDS);
        transport.addListener(new TransportListener() {
            @Override
            public void onAccept(Transport transport, SocketAddress remoteAddress) {
            }
            @Override
            public void onConnect(Transport transport, SocketAddress remoteAddress) {
            }
            @Override
            public void onClose(Transport transport) {
                future.cancel(true);
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
