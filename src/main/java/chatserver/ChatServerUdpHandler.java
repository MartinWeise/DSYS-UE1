package chatserver;

import util.Config;
import util.Log;
import util.User;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;

public class ChatServerUdpHandler implements Runnable {

    private DatagramSocket socket;
    private HashMap<Socket, User> users;
    private InputStream inputStream;
    private PrintStream outputStream;

    public ChatServerUdpHandler(DatagramSocket socket, HashMap<Socket, User> users,
                                InputStream inputStream, PrintStream outputStream) {
        this.socket = socket;
        this.users = users;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        try {
            byte[] receiveData;
            // read client requests
            while (true) {
                receiveData = new byte[4096];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                String[] parts = new String(receiveData, 0, receivePacket.getLength()).split(" ");
                //                                       ^  ^ very important, otherwise not equal later
                switch (parts[0]) {
                    case "!list":
                        list(socket, receivePacket);
                        break;
                    default:
                        throw new RuntimeException("Unknown command!");
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("IOException while UDP", e);
        } finally {
            if (socket != null && !socket.isClosed())
                users.remove(socket);
            try {
                socket.close();
            } catch (NullPointerException e) {
                // Ignored because we cannot handle it
            }
        }
    }

    private void list(DatagramSocket socket, DatagramPacket packet) throws IOException {
        String out = "Online users:";
        for (User u : users.values()) {
            out += "\n* " + u.getName();
        }
        byte[] data = out.getBytes();
        DatagramPacket send = new DatagramPacket(data, data.length, packet.getSocketAddress());
        //System.out.println("UDP -> " + send.getAddress().toString() + " : " + send.getPort());
        socket.send(send);
    }

}
