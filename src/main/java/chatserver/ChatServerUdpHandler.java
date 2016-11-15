package chatserver;

import util.Config;
import util.Log;
import util.User;

import java.io.*;
import java.net.*;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                /* blocking I/O here */
                socket.receive(packet);
                String[] parts = new String(packet.getData(), 0, packet.getLength()).split(" ");
                //                                       ^  ^ very important, otherwise not equal later
                switch (parts[0]) {
                    case "!list":
                        try {
                            list(packet);
                        } catch (IOException e) {
                            throw new RuntimeException("list", e);
                        }
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

    private synchronized void list(DatagramPacket packet) throws IOException {
        /* Collator implements Comperator => sort alphabetical */
        Collection<String> users = new TreeSet<>(Collator.getInstance());
        for (User u : this.users.values()) {
            users.add(u.getName());
        }
        String out = "Online users:";
        for (String username : users) {
            out += "\n* " + username;
        }
        byte[] data = out.getBytes();
        DatagramPacket send = new DatagramPacket(data, data.length, packet.getSocketAddress());
        socket.send(send);
        outputStream.println("~> " + packet.getSocketAddress());
    }

}
