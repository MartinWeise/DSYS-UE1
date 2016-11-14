package chatserver;

import util.Config;
import util.Log;
import util.User;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;

public class ChatServerTcpHandler implements Runnable {

    private Socket socket;
    private HashMap<Socket, User> users;
    private InputStream inputStream;
    private PrintStream outputStream;

    public ChatServerTcpHandler(Socket socket, HashMap<Socket, User> users,
                                InputStream inputStream, PrintStream outputStream) {
        this.socket = socket;
        this.users = users;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        try {
            // prepare the input reader for the socket
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            // prepare the writer for responding to clients requests
            PrintWriter writer = new PrintWriter(socket.getOutputStream(),
                    true);

            String request;
            // read client requests
            while (true) {

                if ((request = reader.readLine()) != null) {
                    outputStream.println("Client sent the following request: " + request);

                    /*
                     * check if request has the correct format: !ping
                     * <client-name>
                     */
                    String[] parts = request.split(" ");
                    switch (parts[0]) {
                        case "!login":
                            login(parts, reader, writer, socket);
                            break;
                        case "!logout":
                            logout(reader, writer, socket);
                            break;
                        case "!send":
                            // attach {request string}\{"!send "}
                            send(request.substring(parts[0].length() + 1, request.length()), reader, writer, socket);
                            break;
                        case "!lookup":
                            lookup(request.substring(parts[0].length() + 1, request.length()), reader, writer, socket);
                            break;
                        case "!register":
                            register(request.substring(parts[0].length() + 1, request.length()), reader, writer, socket);
                            break;
                        default:
                            writer.println("!error");
                            throw new RuntimeException("No such operation: " + parts[0]);
                    }
                    outputStream.println("# of clients: " + users.size() + " & " + Arrays.toString(users.values().toArray()));
                }
            }

        } catch (IOException | NullPointerException e) {
            outputStream.println("Error occurred while waiting for/communicating with client: " + e.getMessage() + "\n");
        } finally {
            try {
                users.remove(socket);
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException | NullPointerException e) {
                // Ignored because we cannot handle it
                throw new RuntimeException("Socket couldn't be closed", e);
            }
        }
    }

    private void login(String[] parts, BufferedReader reader, PrintWriter writer, Socket socket) {
        Config config = new Config("user");
        // check if parts[1]=username already known + password entered is correct
        if (users.containsKey(socket)
                && config.getString(parts[1] + ".password").equals(parts[2])) {
            // already logged in
            if (users.get(socket).isOnline()) {
                writer.println("Already logged in.");
                return;
            }
            // re-logged in
            users.get(socket).setOnline();
            writer.println("Successfully logged in.");
            return;
        }
        // check (quick'n'dirty) if user exists in config file
        if (config.listKeys().contains(parts[1] + ".password")
                && config.getString(parts[1] + ".password").equals(parts[2])) {
            users.put(socket, new User(parts[1]));
            writer.println("Successfully logged in.");
            return;
        }
        writer.println("Wrong username or password.");
    }

    private void logout(BufferedReader reader, PrintWriter writer, Socket socket) throws IOException {
        if (users.containsKey(socket)) {
            users.get(socket).setOffline();
            writer.println("Successfully logged out.");
            socket.close();
            return;
        }
        writer.println("Not logged in.");
    }

    private void send (String msg, BufferedReader reader, PrintWriter writer, Socket socket) throws IOException {
        // is user eligible to send broadcasting messages?
        if (users.containsKey(socket)) {
            for (Socket s : users.keySet()) {
                // send to all users except the demanding one
                if (!s.equals(socket) && users.get(s).isOnline()) {
                    PrintWriter userWriter = new PrintWriter(s.getOutputStream(), true);
                    userWriter.println(users.get(socket).getName() + ": " + msg);
                    /* do not close here -> would lead to closing of all socket I/O */
                }
            }
            return;
        }
        // TODO: error case
        writer.println("Not logged in.");
    }

    private void lookup (String name, BufferedReader reader, PrintWriter writer, Socket socket) throws IOException {
        if (!users.containsKey(socket)) {
            writer.println ("Not logged in.");
            return;
        }
        User found = null;
        for (User u : users.values()) {
            if (u.getName().equals(name) && u.getPrivateAddress() != null) {
                found = u;
                break;
            }
        }
        if (found != null) {
            writer.println(found.getPrivateAddress());
            return;
        }
        writer.println("Wrong username or user not registered.");
    }

    private void register (String privateAddress, BufferedReader reader, PrintWriter writer, Socket socket) throws IOException {
        if (!users.containsKey(socket)) {
            writer.println("Not logged in.");
            return;
        }
        users.get(socket).register(privateAddress);
        writer.println("Successfully registered address for " + users.get(socket).getName() + ".");
        outputStream.println(privateAddress);
    }

}
