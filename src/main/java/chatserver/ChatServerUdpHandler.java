package chatserver;

import util.Config;
import util.Log;
import util.User;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServerUdpHandler implements Runnable {

    private DatagramSocket socket;
    private HashMap<Socket, User> users;
    private InputStream inputStream;
    private PrintStream outputStream;

    private ExecutorService pool;

    public ChatServerUdpHandler(DatagramSocket socket, HashMap<Socket, User> users,
                                InputStream inputStream, PrintStream outputStream) {
        this.socket = socket;
        this.users = users;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.pool = Executors.newCachedThreadPool();
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
                /* [...] each client connection should also be handled in a seperate thread. */
                pool.submit(new ChatServerUdpRequestHandler(receivePacket, socket, users));
            }

        } catch (IOException e) {
//            if (!shutdown)
//                throw new RuntimeException("IOException while UDP", e);
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

}
