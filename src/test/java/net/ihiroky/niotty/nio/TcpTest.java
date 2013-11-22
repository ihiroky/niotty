package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.Pipeline;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StageKeys;
import net.ihiroky.niotty.Transport;
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
    private TransportStatusListener clientStatusListener_;

    private static final int SLEEP_MILLIS = 10;
    private static final InetSocketAddress SERVER_ENDPOINT = new InetSocketAddress(12345);

    private static class TransportStatusListener extends LoadStage {

        boolean connected_;
        boolean loadClosed_;
        boolean storeClosed_;
        boolean wholeClosed_;
        volatile Transport transport_;

        @Override
        public void loaded(StageContext context, Object input, Object parameter) {
        }

        @Override
        public void exceptionCaught(StageContext context, Exception exception) {
        }

        @Override
        public void activated(StageContext context) {
            synchronized (this) {
                connected_ = true;
                notify();
            }
            transport_ = context.transport();
        }

        @Override
        public void deactivated(StageContext context, DeactivateState state) {
            switch (state) {
                case LOAD:
                    synchronized (this) {
                        loadClosed_ = true;
                        notify();
                    }
                    break;
                case STORE:
                    synchronized (this) {
                        storeClosed_ = true;
                        notify();
                    }
                    break;
                case WHOLE:
                    synchronized (this) {
                        wholeClosed_ = true;
                        notify();
                    }
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        serverStatusListener_ = new TransportStatusListener();
        serverSocketProcessor_ = new NioServerSocketProcessor()
                .setPipelineComposer(new PipelineComposer() {
                    @Override
                    public void compose(Pipeline pipeline) {
                        pipeline.add(StageKeys.of("Listener"), serverStatusListener_);
                    }
                });
        clientStatusListener_ = new TransportStatusListener();
        clientSocketProcessor_ = new NioClientSocketProcessor()
                .setPipelineComposer(new PipelineComposer() {
                    @Override
                    public void compose(Pipeline pipeline) {
                        pipeline.add(StageKeys.of("Listener"), clientStatusListener_);
                    }
                });
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

    private static void waitUntilConnected(final TransportStatusListener tsl) throws InterruptedException {
        synchronized (tsl) {
            while (!tsl.connected_) {
                tsl.wait();
            }
        }
    }

    private static void waitWhileConnected(TransportStatusListener tsl) throws InterruptedException {
        synchronized (tsl) {
            while (!tsl.wholeClosed_) {
                tsl.wait();
            }
        }
    }

    private static void waitUntilShutdownInput(TransportStatusListener tsl) throws InterruptedException {
        synchronized (tsl) {
            while (!tsl.loadClosed_) {
                tsl.wait();
            }
        }
    }

    private static void waitUntilShutdownOutput(TransportStatusListener tsl) throws InterruptedException {
        synchronized (tsl) {
            while (!tsl.storeClosed_) {
                tsl.wait();
            }
        }
    }

    private static void waitUntilClosed(Transport transport) throws InterruptedException {
        while (transport.isOpen()) {
            Thread.sleep(SLEEP_MILLIS);
        }
    }

    @Test(timeout = 3000)
    public void testShutdownOutput() throws Exception {
        serverSut_.bind(SERVER_ENDPOINT).await();
        clientSut_.connect(SERVER_ENDPOINT).await();
        waitUntilConnected(serverStatusListener_);

        clientSut_.shutdownOutput();
        waitUntilShutdownInput(serverStatusListener_);
        waitUntilShutdownOutput(clientStatusListener_);
    }

    @Test(timeout = 3000)
    public void testBlockingConnect() throws Exception {
        NioClientSocketProcessor p = new NioClientSocketProcessor();
        p.setUseNonBlockingConnection(false);
        try {
            p.start();
            NioClientSocketTransport clientSut = p.createTransport();

            serverSut_.bind(SERVER_ENDPOINT).await();
            clientSut.connect(SERVER_ENDPOINT).await();
            waitUntilConnected(serverStatusListener_);

            clientSut.close();

            // check client transport state in the server
            waitUntilShutdownInput(serverStatusListener_);
            waitUntilClosed(clientSut);
        } finally {
            p.stop();
        }
    }
}
