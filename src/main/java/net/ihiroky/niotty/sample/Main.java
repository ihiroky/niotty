package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.nio.NioClientSocketConfig;
import net.ihiroky.niotty.nio.NioClientSocketProcessor;
import net.ihiroky.niotty.nio.NioServerSocketConfig;
import net.ihiroky.niotty.nio.NioServerSocketProcessor;

import java.net.InetSocketAddress;

/**
 * Created on 13/01/18, 12:59
 *
 * @author Hiroki Itoh
 */
public class Main {

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
            serverConfig.setPipelineInitializer(new ServerPipelineInitializer());
            serverTransport = server.createTransport(serverConfig);
            serverTransport.bind(new InetSocketAddress(port));

            NioClientSocketConfig clientConfig = new NioClientSocketConfig();
            clientConfig.setPipelineInitializer(new ClientPipelineInitializer());
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
