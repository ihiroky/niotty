package net.ihiroky.niotty.nio;

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

    private static final int SLEEP_MILLIS = 10;
    private static final InetSocketAddress SERVER_ENDPOINT = new InetSocketAddress(12345);

    @Before
    public void setUp() throws Exception {

        serverSocketProcessor_ = new NioServerSocketProcessor();
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

    private static void waitUntilConnected(NioServerSocketTransport serverSut) throws InterruptedException {
        while (serverSut.childSet().isEmpty()) {
            Thread.sleep(SLEEP_MILLIS);
        }
    }

    private static void waitWhileConnected(NioServerSocketTransport serverSut) throws InterruptedException {
        while (!serverSut.childSet().isEmpty()) {
            System.out.println(serverSut.childSet());
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
        waitUntilConnected(serverSut_);

        // TODO use TransportStateEvent.SHUTDOWN_OUTPUT
        clientSut_.shutdownOutput();
        waitWhileConnected(serverSut_);
        waitUntilClosed(clientSut_);
    }

    @Test(timeout = 3000)
    public void testShutdownInput() throws Exception {
        serverSut_.bind(SERVER_ENDPOINT).waitForCompletion();
        clientSut_.connect(SERVER_ENDPOINT).waitForCompletion();
        waitUntilConnected(serverSut_);

        // TODO use TransportStateEvent.INPUT_OUTPUT
        clientSut_.shutdownInput();
        waitWhileConnected(serverSut_);
        waitUntilClosed(clientSut_);
    }

    @Test//(timeout = 3000)
    public void testBlockingConnect() throws Exception {
        NioClientSocketProcessor p = new NioClientSocketProcessor();
        p.setNumberOfConnectThread(0);
        try {
            p.start();
            NioClientSocketTransport clientSut = p.createTransport();

            serverSut_.bind(SERVER_ENDPOINT).waitForCompletion();
            clientSut.connect(SERVER_ENDPOINT).waitForCompletion();
            waitUntilConnected(serverSut_);

            clientSut.close();
            waitWhileConnected(serverSut_);
            waitUntilClosed(clientSut);
        } finally {
            p.stop();
        }
    }
}
