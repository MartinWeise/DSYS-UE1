package client;

import java.io.*;
import java.net.*;

import cli.Command;
import cli.Shell;
import util.Config;
import util.Log;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private Shell shell;
	Socket socket = null;

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		/*
		 * First, create a new Shell instance and provide the name of the
		 * component, an InputStream as well as an OutputStream. If you want to
		 * test the application manually, simply use System.in and System.out.
		 */
		this.shell = new Shell(componentName, userRequestStream, userResponseStream);
		/*
		 * Next, register all commands the Shell should support. In this example
		 * this class implements all desired commands.
		 */
		this.shell.register(this);
	}

	@Override
	public void run() {
		/*
		 * Finally, make the Shell process the commands read from the
		 * InputStream by invoking Shell.run(). Note that Shell implements the
		 * Runnable interface. Thus, you can run the Shell asynchronously by
		 * starting a new Thread:
		 *
		 * Thread shellThread = new Thread(shell); shellThread.start();
		 *
		 * In that case, do not forget to terminate the Thread ordinarily.
		 * Otherwise, the program will not exit.
		 */
		new Thread(shell).start();
		System.out.println(getClass().getName()
				+ " up and waiting for commands!");
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {
		if (socket == null) {
			socket = new Socket(config.getString("chatserver.host"),
					config.getInt("chatserver.tcp.port"));
		}
		// create a reader to retrieve messages send by the server
		BufferedReader serverReader = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));
		// create a writer to send messages to the server
		PrintWriter serverWriter = new PrintWriter(
				socket.getOutputStream(), true);
		new Log("Sending request: " + "!login " + username + " " + password);
		serverWriter.println("!login " + username + " " + password);
		String response = serverReader.readLine();
		new Log(response);
		System.out.println(response);
		return null;
	}

	@Override
	@Command
	public String logout() throws IOException {
		if (socket == null) {
			socket = new Socket(config.getString("chatserver.host"),
					config.getInt("chatserver.tcp.port"));
		}
		// create a reader to retrieve messages send by the server
		BufferedReader serverReader = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));
		// create a writer to send messages to the server
		PrintWriter serverWriter = new PrintWriter(
				socket.getOutputStream(), true);
		new Log("Sending request: !logout");
		serverWriter.println("!logout");
		System.out.println(serverReader.readLine());
		return null;
	}

	@Override
	@Command
	public String send(String message) throws IOException {
		if (socket == null) {
			socket = new Socket(config.getString("chatserver.host"),
					config.getInt("chatserver.tcp.port"));
		}
		// create a reader to retrieve messages send by the server
//		BufferedReader serverReader = new BufferedReader(
//				new InputStreamReader(socket.getInputStream()));
		// create a writer to send messages to the server
		PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);
		serverWriter.println("!send " + message);
		return null;
	}

	@Override
	@Command
	public String list() throws IOException {
		int port = 22021;
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName("localhost");
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		String sentence = "hello from udp";
		sendData = sentence.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
		clientSocket.send(sendPacket);
		System.out.println("package sent.");
		return null;
	}

	@Override
	public String msg(String username, String message) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lookup(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String register(String privateAddress) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String lastMsg() throws IOException {
//		if (socket == null) {
//			socket = new Socket(config.getString("chatserver.host"),
//					config.getInt("chatserver.tcp.port"));
//		}
//		BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//		return serverReader.readLine();
		return null;
	}

	@Override
	@Command
	public String exit() throws IOException {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		}
		this.shell.close();
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);
		// TODO: start the client
		client.run();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
