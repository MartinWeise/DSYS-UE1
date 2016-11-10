//package chatserver;
//
//import util.Config;
//import util.Log;
//import util.User;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.Arrays;
//import java.util.HashMap;
//
//public class TcpThread extends Thread {
//
//    private Socket socket;
//    private HashMap<Socket, User> users;
//
//    public TcpThread(Socket socket, HashMap<Socket, User> users) {
//        this.socket = socket;
//        this.users = users;
//    }
//
//    public void run() {
//
//        new Log("Started new TCP thread.");
//        try {
//            // wait for Client to connect
//            // prepare the input reader for the socket
//            BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(socket.getInputStream()));
//            // prepare the writer for responding to clients requests
//            PrintWriter writer = new PrintWriter(socket.getOutputStream(),
//                    true);
//
//            String request;
//            // read client requests
//            while ((request = reader.readLine()) != null) {
//
//                System.out.println("Client sent the following request: " + request);
//                new Log("Client sent request: " + request);
//
//                /*
//                 * check if request has the correct format: !ping
//                 * <client-name>
//                 */
//                String[] parts = request.split(" ");
//                new Log("Request: " + Arrays.toString(parts));
//
//                switch(parts[0]) {
//                    case "!login":
//                        login(parts, reader, writer, socket);
//                        break;
//                    case "!logout":
//                        logout(reader, writer, socket);
//                        break;
//                    case "!send":
//                        // attach {request string}\{"!send "}
//                        send(request.substring(parts[0].length()+1,request.length()),reader,writer,socket);
//                        break;
//                    default:
//                        new Log("No such operation: " + parts[0]);
//                        writer.println("!error");
//                        throw new RuntimeException("No such operation: " + parts[0]);
//                }
//                System.out.println("# of clients: " + users.size() + " & " + Arrays.toString(users.values().toArray()));
//            }
//
//        } catch (IOException | NullPointerException e) {
//            System.err.println("Error occurred while waiting for/communicating with client: " + e.getMessage() + "\n");
//            new Log("Error occurred while waiting for/communicating with client: " + e.getMessage());
//        } finally {
//            if (socket != null && !socket.isClosed())
//                users.remove(socket);
//                try {
//                    socket.close();
//                } catch (IOException | NullPointerException e) {
//                    // Ignored because we cannot handle it
//                }
//        }
//    }
//
//    private void login(String[] parts, BufferedReader reader, PrintWriter writer, Socket socket) {
//        Config config = new Config("user");
//        // check if parts[1]=username already known + password entered is correct
//        if (users.containsKey(socket)
//                && config.getString(parts[1] + ".password").equals(parts[2])) {
//            // already logged in
//            if (users.get(socket).isOnline()) {
//                new Log("Already logged in.");
//                writer.println("Already logged in.");
//                return;
//            }
//            // re-logged in
//            users.get(socket).setOnline();
//            new Log("Successfully logged in.");
//            writer.println("Successfully logged in.");
//            return;
//        }
//        // check (quick'n'dirty) if user exists in config file
//        if (config.listKeys().contains(parts[1] + ".password")
//                && config.getString(parts[1] + ".password").equals(parts[2])) {
//            users.put(socket, new User(parts[1]));
//            new Log("Successfully logged in.");
//            writer.println("Successfully logged in.");
//            return;
//        }
//        new Log("Wrong username or password.");
//        writer.println("Wrong username or password.");
//    }
//
//    private void logout(BufferedReader reader, PrintWriter writer, Socket socket) {
//        if (users.containsKey(socket)) {
//            users.get(socket).setOffline();
//            writer.println("Successfully logged out.");
//            return;
//        }
//        writer.println("Not logged in.");
//    }
//
//    private void send(String msg, BufferedReader reader, PrintWriter writer, Socket socket) {
//        // is user eligible to send broadcasting messages?
//        if (users.containsKey(socket)) {
//            for (Socket s : users.keySet()) {
//                // send to all users except the demanding one
//                if (!s.equals(socket)) {
//                    if (s.isClosed()) {
//                        users.remove(s);
//                        continue;
//                    }
//                    try {
//                        PrintWriter tempWriter = new PrintWriter(s.getOutputStream(), true);
//                        tempWriter.println(users.get(s).getName() + ": " + msg);
//                        tempWriter.close();
//                    } catch (IOException e) {
//                        throw new RuntimeException("Error while sending msg", e);
//                    }
//                }
//            }
//            return;
//        }
//        // TODO: error case
//        writer.println("Not logged in.");
//    }
//
//}
