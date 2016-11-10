package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientListenHandler implements Runnable {

    private Socket socket;

    public ClientListenHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        System.out.println("Hello from ClientListenHandler");
        if (socket.isClosed()) {
            System.err.println("No connection to server. Socket is closed.");
        }
        while (true) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String in = reader.readLine();
                if (in != null) {
                    System.out.println(in);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
