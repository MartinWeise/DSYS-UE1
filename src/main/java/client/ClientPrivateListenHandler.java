package client;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientPrivateListenHandler implements Runnable {

    private InputStream inputStream;
    private PrintStream outputStream;
    private Socket socket;

    public ClientPrivateListenHandler(Socket socket, InputStream inputStream, PrintStream outputStream) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        try {
            // prepare the input reader for the socket
            BufferedReader reader = null;
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            // prepare the writer for responding to clients requests
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            String request;
            while (!socket.isClosed()) {
                if ((request = reader.readLine()) != null) {
                    outputStream.println("Client sent the following request: " + request);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("privateTcpListen", e);
        }
    }
}
