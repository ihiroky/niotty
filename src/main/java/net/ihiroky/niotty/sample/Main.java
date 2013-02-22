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

        NioServerSocketProcessor server = new NioServerSocketProcessor();
        NioClientSocketProcessor client = new NioClientSocketProcessor();
        server.start();
        client.start();
        Transport serverTransport = null;
        Transport clientTransport = null;
        try {
            NioServerSocketConfig serverConfig = new NioServerSocketConfig();
            serverConfig.setPipeLineFactory(new ServerPipeLineFactory());
            serverTransport = server.createTransport(serverConfig);
            serverTransport.bind(new InetSocketAddress(10000));

            NioClientSocketConfig clientConfig = new NioClientSocketConfig();
            clientConfig.setPipeLineFactory(new ClientPipeLineFactory());
            clientTransport = client.createTransport(clientConfig);
            TransportFuture connectFuture = clientTransport.connect(new InetSocketAddress("localhost", 10000));
            connectFuture.waitForCompletion();
            System.out.println("connection wait gets done.");

            Thread.sleep(500);
            serverTransport.write("broadcast from server in thread " + Thread.currentThread());

            Thread.sleep(500);
            System.out.println("type enter to finish.");
            System.in.read();
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
