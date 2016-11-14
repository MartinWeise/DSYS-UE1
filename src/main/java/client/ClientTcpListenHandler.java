package client;

import java.io.*;
import java.net.Socket;

public class ClientTcpListenHandler implements Runnable {

    private Socket socket;
    private InputStream inputStream;
    private PrintStream outputStream;
    private String lastMessage;
    private boolean nextIsPrivateAddress = false;
    private String privateAddress = null;

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
                if (reader.ready()) {
                    String in = reader.readLine();
                    lastMessage = in;
                    if (in != null) {
                        lastMessage = in;
                        if (!nextIsPrivateAddress) {
                            outputStream.println(in);
                        } else {
                            /* set back to false so chat keeps showing */
                            nextIsPrivateAddress = false;
                            privateAddress = new String(in);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("socket",e);
            }
        }
    }

    public synchronized String getLastMessage() {
        return this.lastMessage;
    }

    public synchronized String getPrivateAddress() {
        nextIsPrivateAddress = true;
        while (privateAddress == null) {
            /* just block, run() makes all the work */
        }
        return privateAddress;
    }
}
