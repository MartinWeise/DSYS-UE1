package client;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ClientPrivateListenHandler implements Runnable {

    private PrintStream outputStream;
    private ServerSocket serverSocket;

    /**
     * @brief Constructor needed by {@link Client}.
     * @param serverSocket Private TCP Socket
     * @param outputStream PrintStream to write output
     */
    public ClientPrivateListenHandler(ServerSocket serverSocket, PrintStream outputStream) {
        this.serverSocket = serverSocket;
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
            /*
             Was ist der Output auf 2 aufeinanderfolgende !msg Befehle?
             von Gleichweit Julia - Mittwoch, 9. November 2016, 11:09

             Der Befehl !register soll auf dem Client einen eigenen ServerSocket erstellen.
             Dieses ServerSocket bleibt nach dem erfolgreichen Absenden des !register-Befehles
             bis zum Beenden des Clients aktiv.
             */
            String request;
            while (true) {
                Socket secretSocket = serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(secretSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(secretSocket.getOutputStream(), true);
                if ((request = reader.readLine()) != null) {
                    /* private message was sent, reply with !ack */
                    outputStream.println(request);
                    writer.println("!ack");
                    secretSocket.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("privateTcpListen", e);
        }
    }
}
