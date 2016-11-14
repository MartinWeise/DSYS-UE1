package client;

import java.io.*;
import java.net.Socket;

public class ClientTcpListenHandler implements Runnable {

    private Socket socket;
    private InputStream inputStream;
    private PrintStream outputStream;
    private volatile String lastMsg;

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
        while (true) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String in = reader.readLine();
                lastMsg = in;
                if (in != null) {
                    outputStream.println(in);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized String getLastMsg() {
        return this.lastMsg;
    }
}
