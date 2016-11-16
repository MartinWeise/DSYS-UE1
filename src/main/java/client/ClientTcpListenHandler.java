package client;

import java.io.*;
import java.net.Socket;

public class ClientTcpListenHandler implements Runnable {

    private Socket socket;
    private PrintStream outputStream;
    private String lastMessage;
    private boolean nextIsPrivateAddress = false;
    private boolean shutdown = false;
    private String privateAddress = null;

    private final String LOGOUT_MSG_SUCCESS = "Successfully logged out.";

    /**
     * @brief The Constructor needed for {@link Client}.
     * @param socket The client Socket.
     * @param outputStream The PrintStream to write output at.
     */
    public ClientTcpListenHandler(Socket socket, PrintStream outputStream) {
        this.socket = socket;
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
                if (true) {
                    String in = reader.readLine();
                    if (!nextIsPrivateAddress) {
                        outputStream.println(in);
                        if (in.indexOf(':') != -1) {
                            /* it is a public message if it has at least one ':' */
                            lastMessage = in;
                        }
                    } else {
                        /* set back to false so chat keeps showing */
                        nextIsPrivateAddress = false;
                        privateAddress = in;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("socket",e);
            }
        }
    }

    /**
     * @brief Gets the last public message.
     * @return Last public message
     */
    public synchronized String getLastMessage() {
        return this.lastMessage;
    }

    /**
     * @brief Waits for the private message.
     * @detail Heavily relies on {@method run}.
     * @return The private Message.
     */
    public synchronized String getPrivateAddress() {
        nextIsPrivateAddress = true;
        while (privateAddress == null) {
            /* just block, run() makes all the work */
        }
        return privateAddress;
    }

    /**
     * @brief Turn-off the Client program.
     * @detail Wait for successful logout message.
     * @return
     */
    public boolean shutdownOnSuccess() {
        shutdown = true;
        nextIsPrivateAddress = true; /* surpress output */
        while (lastMessage != null && !lastMessage.equals(LOGOUT_MSG_SUCCESS)) {
            /* just block, run() makes all the work */
        }
        outputStream.print(LOGOUT_MSG_SUCCESS);
        return true;
    }
}
