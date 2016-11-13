package client;


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

    public ClientUdpListenHandler(DatagramSocket socket, InputStream inputStream, PrintStream outputStream) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public void run() {
        outputStream.println("Hello from UDP Thread.");
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            socket.receive(receivePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
        outputStream.println(response);
    }

}
