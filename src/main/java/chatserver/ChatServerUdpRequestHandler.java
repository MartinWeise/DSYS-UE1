package chatserver;

import util.User;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.HashMap;

public class ChatServerUdpRequestHandler implements Runnable {

    private DatagramPacket packet;
    private DatagramSocket socket;
    private HashMap<Socket, User> users;

    public ChatServerUdpRequestHandler (DatagramPacket packet, DatagramSocket socket, HashMap<Socket, User> users) {
        this.packet = packet;
        this.socket = socket;
        this.users = users;
    }

    @Override
    public void run() {
        String[] parts = new String(packet.getData(), 0, packet.getLength()).split(" ");
        //                                       ^  ^ very important, otherwise not equal later
        switch (parts[0]) {
            case "!list":
                try {
                    list();
                } catch (IOException e) {
//                    throw new RuntimeException("list", e);
                }
                break;
            default:
                throw new RuntimeException("Unknown command!");
        }
    }

    private synchronized void list() throws IOException {
        String out = "Online users:";
        for (User u : users.values()) {
            out += "\n* " + u.getName();
        }
        byte[] data = out.getBytes();
        DatagramPacket send = new DatagramPacket(data, data.length, packet.getSocketAddress());
        System.out.println("UDP -> " + send.getAddress().toString() + " : " + send.getPort());
        socket.send(send);
    }
}
