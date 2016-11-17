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

    private DatagramSocket socket = null;
    private HashMap<Socket, User> users;

    /**
     * @brief Constructor needed in {@link Chatserver}.
     * @param socket The server Socket.
     * @param users The {@link HashMap<Socket,User>} used to store the users.
     */
    public ChatServerUdpHandler(DatagramSocket socket, HashMap<Socket, User> users) {
        this.socket = socket;
        this.users = users;
    }

    /**
     * @brief The thread entry point & working point.
     * @detail Need an open {@link DatagramSocket}.
     * @throws RuntimeException
     *              When Socket sending/receiving/closing fails.
     */
    @Override
    public void run() {
        try {
            byte[] receiveData;
            while (!socket.isClosed()) {
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

    /**
     * @brief Send a reply to the client with the user list via UDP
     * @detail Users are printed alphabetical A-Z
     * @param packet
     * @throws IOException
     */
    private synchronized void list(DatagramPacket packet) throws IOException {
        /* Collator implements Comperator => sort alphabetical */
        Collection<String> users = new TreeSet<>(Collator.getInstance());
        for (User u : this.users.values()) {
            if (u.isOnline()) {
                users.add(u.getName());
            }
        }
        String out = "Online users:";
        for (String username : users) {
            out += "\n* " + username;
        }
        byte[] data = out.getBytes();
        DatagramPacket send = new DatagramPacket(data, data.length, packet.getSocketAddress());
        socket.send(send);
    }

}
