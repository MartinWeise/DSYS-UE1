package client;


import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientUdpListenHandler extends Thread {

    private DatagramSocket socket;
    private InputStream inputStream;
    private PrintStream outputStream;
    private Config config;

    public ClientUdpListenHandler(Config config, DatagramSocket socket, InputStream inputStream, PrintStream outputStream) {
        this.config = config;
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public void run() {
        byte[] receiveData = new byte[4096];
        DatagramPacket receivePacket;
        while (true) {
            try {
                receivePacket = new DatagramPacket(receiveData, receiveData.length, InetAddress.getByName(config.getString("chatserver.host")),
                        config.getInt("chatserver.udp.port"));
                socket.receive(receivePacket);
            } catch (IOException e) {
                throw new RuntimeException("recv", e);
            }
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            outputStream.println(response);
        }
    }

}
