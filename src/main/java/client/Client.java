package client;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import cli.Command;
import cli.Shell;
import util.Config;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream inputStream;
	private PrintStream outputStream;

	private Shell shell;
	private String lastMessage = null;

	private DatagramSocket udpSocket = null;
	private ExecutorService pool;
	private Socket tcpSocket = null;
	/* used for holding IOException back when closing the server */
	private boolean shutdown = false;
	private ServerSocket privateChatServer = null;
	private Future privateChatServerHandler = null;


	private final String LASTMSG_EMPTY = "No message received!";
	private final String REGISTERADDR_MALFORMED = "Please provide a valid <IP:port> address.";

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param inputStream
	 *            the input stream to read user input from
	 * @param outputStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config,
			InputStream inputStream, PrintStream outputStream) {
		this.componentName = componentName;
		this.config = config;
		this.inputStream = inputStream;
		this.outputStream = outputStream;

		this.pool = Executors.newFixedThreadPool(3);

		/*
		 * First, create a new Shell instance and provide the name of the
		 * component, an InputStream as well as an OutputStream. If you want to
		 * test the application manually, simply use System.in and System.out.
		 */
		this.shell = new Shell(componentName, inputStream, outputStream);
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
		if (tcpSocket == null) {
			try {
				tcpSocket = new Socket(config.getString("chatserver.host"),
                        config.getInt("chatserver.tcp.port"));
			} catch (IOException e) {
				throw new RuntimeException("Unable to create TCP socket.", e);
			}
		}
		if (udpSocket == null) {
			try {
				udpSocket = new DatagramSocket();
			} catch (SocketException e) {
				throw new RuntimeException("Unable to create UDP socket.", e);
			}
		}
		if (!pool.isShutdown()) {
			pool.submit(new ClientTcpListenHandler(tcpSocket, inputStream, outputStream));
			pool.submit(new ClientUdpListenHandler(udpSocket, inputStream, outputStream));
		}
		outputStream.println(getClass().getName()
				+ " up and waiting for commands!");
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);
		serverWriter.println("!login " + username + " " + password);
		return null;
	}

	@Override
	@Command
	public String logout() throws IOException {
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);
		serverWriter.println("!logout");
		return exit();
	}

	@Override
	@Command
	public String send(String message) throws IOException {
		// create a writer to send messages to the server
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);
		serverWriter.println("!send " + message);
		return null;
	}

	@Command
	public String list() throws IOException {
		byte[] data;
		data = "!list".getBytes();
		DatagramPacket send = new DatagramPacket(data, data.length,
				InetAddress.getByName(config.getString("chatserver.host")),
				config.getInt("chatserver.udp.port"));
		udpSocket.send(send);
		return null;
	}

	@Override
	@Command
	public String msg(String username, String message) throws IOException {
		// create a writer to send messages to the server
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(tcpSocket.getInputStream()));
		String address = lookup(username);

		if (address != null) {
			/* Generate new TCP Socket with user */
			outputStream.println("Yep.");
		}
		outputStream.println("Wrong username or user not reachable.");
		return null;
	}

	@Override
	@Command
	public String lookup(String username) throws IOException {
		// create a writer to send messages to the server
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);

		serverWriter.println("!lookup " + username);
		return null;
	}

	@Override
	@Command
	public String register(String privateAddress) throws IOException {
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);
		serverWriter.println("!register " + privateAddress);
		InetAddress address;
		int port;
		/* try to parse @argument {privateAddress} into inetaddr and port */
		String[] parts = privateAddress.split(":");

		if (parts.length != 2) {
			return REGISTERADDR_MALFORMED;
		}
		address = InetAddress.getByName(parts[0]);
		port = Integer.parseInt(parts[1]);

		closePrivateConnection();
		/* backlog - requested maximum length of the queue of incoming connections. = 10 */
		privateChatServer = new ServerSocket(port, 10, address);
		outputStream.println("aa");
		privateChatServerHandler = pool.submit(new ClientPrivateListenHandler(privateChatServer.accept(), inputStream, outputStream));
		return null;
	}
	
	@Override
	@Command
	public String lastMsg() throws IOException {
		if (lastMessage == null) {
			outputStream.println(LASTMSG_EMPTY);
			return null;
		}
		outputStream.println(lastMessage);
		return null;
	}

	@Override
	@Command
	public String exit() throws IOException {
		shutdown = true;
		/* Close TCP connection */
		tcpSocket.close();
		/* Close private TCP connection */
		closePrivateConnection();
		/* Close UDP connection */
		udpSocket.close();
		/* Shutdown pool */
		pool.shutdown();
		if (!pool.isShutdown()) {
			pool.shutdownNow();
			if (!pool.isShutdown()) {
				throw new RuntimeException("Pool couldn't be shut down.");
			}
		}
		shell.close();
		return "Bye.";
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

	// --- Commands needed for Lab 2. Please note that you dFo not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	private void closePrivateConnection() throws IOException {
		if (privateChatServerHandler != null) {
			privateChatServer.close();
			if (!privateChatServer.isClosed()) {
				throw new RuntimeException("Couldn't close private TCP socket.");
			}
			privateChatServerHandler.cancel(true);
			if (!privateChatServerHandler.isCancelled()) {
				throw new RuntimeException("Couldn't close private TCP handler.");
			}
		}
	}

}
