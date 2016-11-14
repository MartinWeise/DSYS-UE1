package client;

import java.io.*;
import java.net.Socket;

public class ClientTcpListenHandler implements Runnable {

    private Socket socket;
    private InputStream inputStream;
    private PrintStream outputStream;
<<<<<<< HEAD
    private String lastMessage;
=======
    private volatile String lastMsg;
>>>>>>> 81863fb1fc701726f2c7eb3c18ddd6d79934841d

    public ClientTcpListenHandler(Socket socket, InputStream inputStream, PrintStream outputStream) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        if (socket.isClosed()) {
            outputStream.println("No connection to server. Socket is closed.");
        }
        while (!socket.isClosed()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
<<<<<<< HEAD
                if (reader.ready()) {
                    String in = reader.readLine();
                    this.lastMessage = in;
                    if (in != null) {
                        lastMessage = in;
                        outputStream.println(in);
                    }
=======
                String in = reader.readLine();
                lastMsg = in;
                if (in != null) {
                    outputStream.println(in);
>>>>>>> 81863fb1fc701726f2c7eb3c18ddd6d79934841d
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        exit();
    }

    public synchronized String getLastMessage() {
        return this.lastMessage;
    }

    public void exit() {
        // TODO
    }

    public synchronized String getLastMsg() {
        return this.lastMsg;
    }
}
