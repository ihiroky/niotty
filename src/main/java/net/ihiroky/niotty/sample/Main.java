package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Processor;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.nio.NioClientSocketProcessor;
import net.ihiroky.niotty.nio.NioClientSocketConfig;
import net.ihiroky.niotty.nio.NioServerSocketProcessor;
import net.ihiroky.niotty.nio.NioServerSocketConfig;

import java.net.InetSocketAddress;

/**
 * Created on 13/01/18, 12:59
 *
 * @author Hiroki Itoh
 */
public class Main {

    public static void main(String[] args) {

        Processor<NioServerSocketConfig> server = new NioServerSocketProcessor();
        Processor<NioClientSocketConfig> client = new NioClientSocketProcessor();
        server.start();
        client.start();
        Transport serverTransport = null;
        Transport clientTransport = null;
        try {
            NioServerSocketConfig serverConfig = new NioServerSocketConfig(new ServerPipeLineFactory());
            serverTransport = server.createTransport(serverConfig);
            serverTransport.bind(new InetSocketAddress(10000));

            NioClientSocketConfig clientConfig = new NioClientSocketConfig(new ClientPipeLineFactory());
            clientTransport = client.createTransport(clientConfig);
            clientTransport.connect(new InetSocketAddress("localhost", 10000));

            Thread.sleep(500);
            System.out.println("type enter to broadcast from server.");
            System.in.read();
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
