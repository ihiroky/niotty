package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Niotty;
import net.ihiroky.niotty.Processor;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.nio.NioClientSocketBusInterface;
import net.ihiroky.niotty.nio.NioClientSocketConfig;
import net.ihiroky.niotty.nio.NioServerSocketBusInterface;
import net.ihiroky.niotty.nio.NioServerSocketConfig;

import java.net.InetSocketAddress;

/**
 * Created on 13/01/18, 12:59
 *
 * @author Hiroki Itoh
 */
public class Main {

    public static void main(String[] args) {

        Processor<NioServerSocketConfig> server =
                Niotty.newProcessor(new NioServerSocketBusInterface(), new ServerPipeLineFactory());
        Processor<NioClientSocketConfig> client =
                Niotty.newProcessor(new NioClientSocketBusInterface(), new ClientPipeLineFactory());
        server.start();
        client.start();
        Transport serverTransport = null;
        Transport clientTransport = null;
        try {
            serverTransport = server.createTransport();
            serverTransport.bind(new InetSocketAddress(10000));

            clientTransport = client.createTransport();
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
