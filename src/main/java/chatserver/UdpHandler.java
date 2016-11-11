package chatserver;


import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;

public class UdpHandler extends Thread {

    private DatagramSocket socket;
    private InputStream inputStream;
    private PrintStream outputStream;

    public UdpHandler(DatagramSocket socket, InputStream inputStream, PrintStream outputStream) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public void run() {
        outputStream.println("Hello from UDP Thread.");
//        byte[] receiveData = new byte[1024];
//        byte[] sendData = new byte[1024];
//        while(true)
//        {
//            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//            try {
//                socket.receive(receivePacket);
//                String sentence = new String( receivePacket.getData());
//                System.out.println("RECEIVED: " + sentence);
//                InetAddress IPAddress = receivePacket.getAddress();
//                int port = 11021;
//                String capitalizedSentence = sentence.toUpperCase();
//                sendData = capitalizedSentence.getBytes();
//                DatagramPacket sendPacket =
//                        new DatagramPacket(sendData, sendData.length, IPAddress, port);
//                System.out.println("=> " + port);
//                socket.send(sendPacket);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

}
