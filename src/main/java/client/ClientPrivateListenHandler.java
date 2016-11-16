package client;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientPrivateListenHandler implements Runnable {

    private PrintStream outputStream;
    private Socket socket;

    /**
     * @brief Constructor needed by {@link Client}.
     * @param socket Private TCP Socket
     * @param outputStream PrintStream to write output
     */
    public ClientPrivateListenHandler(Socket socket, PrintStream outputStream) {
        this.socket = socket;
        this.outputStream = outputStream;
    }

    /**
     * @brief The thread entry & running method
     * @throws RuntimeException
     * 				Will be thrown since {@link IOException} and {@link SocketException}
     * 			    are much more worse to handle.
     */
    @Override
    public void run() {
        try {
            BufferedReader reader = null;
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            String request;
            while (!socket.isClosed()) {
                if ((request = reader.readLine()) != null) {
                    /* private message was sent, reply with !ack */
                    outputStream.println(request);
                    writer.println("!ack");
                    socket.close();
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("privateTcpListen", e);
        }
    }
}
