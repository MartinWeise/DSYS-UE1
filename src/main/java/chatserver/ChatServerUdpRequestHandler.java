//package chatserver;
//
//import util.User;
//
//import java.io.IOException;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.Socket;
//import java.text.Collator;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.TreeSet;
//
//public class ChatServerUdpRequestHandler implements Runnable {
//
//    private DatagramPacket packet;
//    private DatagramSocket socket;
//    private HashMap<Socket, User> users;
//
//    public ChatServerUdpRequestHandler (DatagramPacket packet, DatagramSocket socket, HashMap<Socket, User> users) {
//        this.packet = packet;
//        this.socket = socket;
//        this.users = users;
//    }
//
//    @Override
//    public void run() {
//
//    }
//}
