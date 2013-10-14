package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StageKeys;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.TransportStateEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;

/**
 * @author Hiroki Itoh
 */
public class TcpTest {

    private NioServerSocketTransport serverSut_;
    private NioClientSocketTransport clientSut_;
    private NioServerSocketProcessor serverSocketProcessor_;
    private NioClientSocketProcessor clientSocketProcessor_;
    private TransportStatusListener serverStatusListener_;

    private static final int SLEEP_MILLIS = 10;
    private static final InetSocketAddress SERVER_ENDPOINT = new InetSocketAddress(12345);

    private static class TransportStatusListener implements LoadStage<Object, Void> {

        volatile boolean connected_;
        volatile boolean closed_;

        @Override
        public void load(StageContext<Void> context, Object input) {
        }

        @Override
        public void load(StageContext<Void> context, TransportStateEvent event) {
            if (event.state() == TransportState.CONNECTED) {
                connected_ = true;
            }
            if (event.state() == TransportState.CLOSED) {
                closed_ = true;
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        serverStatusListener_ = new TransportStatusListener();
        serverSocketProcessor_ = new NioServerSocketProcessor()
                .setPipelineComposer(new PipelineComposer() {
                    @Override
                    public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
                        loadPipeline.add(StageKeys.of("Listener"), serverStatusListener_);
                    }
                });
        clientSocketProcessor_ = new NioClientSocketProcessor();
        serverSocketProcessor_.start();
        clientSocketProcessor_.start();
        serverSut_ = serverSocketProcessor_.createTransport();
        clientSut_ = clientSocketProcessor_.createTransport();
    }

    @After
    public void tearDown() throws Exception {
        clientSut_.close();
        serverSut_.close();
        clientSocketProcessor_.stop();
        serverSocketProcessor_.stop();
    }

    private static void waitUntilConnected(TransportStatusListener tsl) throws InterruptedException {
        while (!tsl.connected_) {
            Thread.sleep(SLEEP_MILLIS);
        }
    }

    private static void waitWhileConnected(TransportStatusListener tsl) throws InterruptedException {
        while (!tsl.closed_) {
            Thread.sleep(SLEEP_MILLIS);
        }
    }

    private static void waitUntilClosed(Transport transport) throws InterruptedException {
        while (transport.isOpen()) {
            Thread.sleep(SLEEP_MILLIS);
        }
    }

    @Test(timeout = 3000)
    public void testShutdownOutput() throws Exception {
        serverSut_.bind(SERVER_ENDPOINT).waitForCompletion();
        clientSut_.connect(SERVER_ENDPOINT).waitForCompletion();
        waitUntilConnected(serverStatusListener_);

        // TODO use TransportStateEvent.SHUTDOWN_OUTPUT
        clientSut_.shutdownOutput();
        waitWhileConnected(serverStatusListener_);
        waitUntilClosed(clientSut_);
    }

    @Test(timeout = 3000)
    public void testShutdownInput() throws Exception {
        serverSut_.bind(SERVER_ENDPOINT).waitForCompletion();
        clientSut_.connect(SERVER_ENDPOINT).waitForCompletion();
        waitUntilConnected(serverStatusListener_);

        // TODO use TransportStateEvent.INPUT_OUTPUT
        clientSut_.shutdownInput();
        waitWhileConnected(serverStatusListener_);
        waitUntilClosed(clientSut_);
    }

    @Test//(timeout = 3000)
    public void testBlockingConnect() throws Exception {
        NioClientSocketProcessor p = new NioClientSocketProcessor();
        p.setUseNonBlockingConnection(false);
        try {
            p.start();
            NioClientSocketTransport clientSut = p.createTransport();

            serverSut_.bind(SERVER_ENDPOINT).waitForCompletion();
            clientSut.connect(SERVER_ENDPOINT).waitForCompletion();
            waitUntilConnected(serverStatusListener_);

            clientSut.close();
            waitWhileConnected(serverStatusListener_);
            waitUntilClosed(clientSut);
        } finally {
            p.stop();
        }
    }
}
