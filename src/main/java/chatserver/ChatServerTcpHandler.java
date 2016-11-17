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
    private PrintStream outputStream;

    /**
     * @brief The constructor used in {@link Chatserver}
     * @param socket
     *              The {@link Socket} that was accepted in {@link Chatserver}.
     * @param users
     *              The {@link HashMap<Socket,User>} that stores the {@link User} Objects
     * @param outputStream
     *              The {@link PrintStream} for printing out command results (e.g. System.in)
     */
    public ChatServerTcpHandler(Socket socket, HashMap<Socket, User> users,
                                PrintStream outputStream) {
        this.socket = socket;
        this.users = users;
        this.outputStream = outputStream;
    }

    /**
     * @brief Runs a CLI for TCP connections
     * @detail Requires {@link Chatserver}
     * @throws RuntimeException
     *              Cannot handle {@link IOException} and {@link NullPointerException}
     *              good enough => throw a RuntimeExeption
     */
    @Override
    public void run() {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader (new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter (socket.getOutputStream(), true);
            String request;

            while (true) {
                if ((request = reader.readLine()) != null) {
                    String[] parts = request.split(" ");
                    switch (parts[0]) {
                        case "!login":
                            login (parts, writer, socket);
                            break;
                        case "!logout":
                            logout (writer, socket);
                            break;
                        case "!send":
                            send (request.substring(parts[0].length() + 1, request.length()), writer, socket);
                            break;
                        case "!lookup":
                            lookup (request.substring(parts[0].length() + 1, request.length()), writer, socket);
                            break;
                        case "!register":
                            register (request.substring(parts[0].length() + 1, request.length()), writer, socket);
                            break;
                        default:
                            writer.println ("!error");
                            throw new RuntimeException ("No such operation: " + parts[0]);
                    }
                }
            }
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Error occurred while waiting for/communicating with client: ", e);
        } finally {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
                if (writer != null) {
                    writer.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException | NullPointerException e) {
                throw new RuntimeException("Socket couldn't be closed", e);
            }
        }
    }

    /**
     * @brief Log-In the requesting user with its {@link Socket}
     * @param parts Array of command instructions.
     * @param writer PrintStream for replies.
     * @param socket The user socket.
     */
    private synchronized void login(String[] parts, PrintWriter writer, Socket socket) {
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
            /* check if user is offline and has a new socket */
            for (User u: users.values()) {
                if (u.getName().equals(parts[1]) && !u.isOnline()) {
                    users.remove(socket);
                }
            }
            users.put(socket, new User(parts[1]));
            writer.println("Successfully logged in.");
            return;
        }
        writer.println("Wrong username or password.");
    }

    /**
     * @brief Log-Out the current user
     * @param writer Output for replies
     * @param socket The user socket.
     * @throws IOException
     *              Can be thrown if the socket closing cause an error.
     */
    private synchronized void logout(PrintWriter writer, Socket socket) throws IOException {
        if (users.containsKey(socket)) {
            users.get(socket).setOffline();
            writer.println("Successfully logged out.");
            return;
        }
        writer.println("Not logged in.");
    }

    /**
     * @brief Sends a public message to all logged-in users.
     * @param msg String that is the message
     * @param writer Output for message.
     * @param socket The user socket.
     * @throws IOException
     *              Can be thrown when the {@link OutputStream} is damaged
     *              due to closed Socket.
     */
    private void send (String msg, PrintWriter writer, Socket socket) throws IOException {
        /* is user eligible to send broadcasting messages? */
        if (users.containsKey(socket)) {
            for (Socket s : users.keySet()) {
                /* send to all users except the demanding one */
                if (!s.equals(socket) && users.get(s).isOnline()) {
                    PrintWriter userWriter = new PrintWriter(s.getOutputStream(), true);
                    userWriter.println(users.get(socket).getName() + ": " + msg);
                    /* do not close here -> would lead to closing of all socket I/O */
                }
            }
            return;
        }
        writer.println("Not logged in.");
    }

    /**
     * @brief Performs a lookup for the private address
     * @detail Requires {@method register} first for sane results.
     * @param name The user whos private address should be queried.
     * @param writer PrintWriter for responses.
     * @param socket The user Socket.
     */
    private void lookup (String name, PrintWriter writer, Socket socket) {
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

    /**
     * @brief Registers the users private messaging address.
     * @detail Makes {@method lookup} sane.
     * @param privateAddress String <IP:Port> that represents the private Address
     * @param writer PrintWriter for responses.
     * @param socket The user Socket.
     */
    private void register (String privateAddress, PrintWriter writer, Socket socket) {
        if (!users.containsKey(socket)) {
            writer.println("Not logged in.");
            return;
        }
        users.get(socket).register(privateAddress);
        writer.println("Successfully registered address for " + users.get(socket).getName() + ".");
        outputStream.println(privateAddress);
    }

}
