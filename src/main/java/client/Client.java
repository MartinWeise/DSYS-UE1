package client;

import java.io.*;
import java.net.*;
import java.nio.Buffer;
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

	private DatagramSocket udpSocket = null;
	private ExecutorService pool;
	private Socket tcpSocket = null;
	/* used for holding IOException back when closing the server */
	private boolean shutdown = false;
	private ServerSocket privateChatServer = null;
	private Future privateChatServerHandler = null;
	private ClientTcpListenHandler tcpListener = null;
	private String username; // for private messages


	private final String LASTMSG_EMPTY = "No message received!";
	private final String REGISTERADDR_MALFORMED = "Please provide a valid <IP:port> address.";
	private final String SECRADDR_MALFORMED = "Wrong username or user not reachable.";

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
		this.shell = new Shell(componentName, inputStream, outputStream);
		this.shell.register(this);

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
				/* has to be empty constructor => Server already uses address */
				udpSocket = new DatagramSocket();
			} catch (SocketException e) {
				throw new RuntimeException("Unable to create UDP socket.", e);
			}
		}
		if (!pool.isShutdown()) {
			pool.submit(tcpListener = new ClientTcpListenHandler(tcpSocket, outputStream));
			pool.submit(new ClientUdpListenHandler(config, udpSocket, outputStream));
		}
		outputStream.println(getClass().getName() + " up and waiting for commands!");
	}

	/**
	 * @brief The thread entry & running method
	 */
	@Override
	public void run() {}

	/**
	 * @brief Log-In the user
	 * @detail Just sends the command and the server does all the work though.
	 * @param username
	 *            The name of the user
	 * @param password
	 *            The password
	 * @return
	 * @throws IOException
	 * 				The {@link Socket} can throw this exception when the
	 * 				{@link OutputStream} is damaged (e.g. Socket closed)
	 */
	@Override
	@Command
	public synchronized String login(String username, String password) throws IOException {
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);
		this.username = username;
		serverWriter.println("!login " + username + " " + password);
		return null;
	}

	/**
	 * @brief Log-out the user and exit program.
	 * @detail Just sends the command and the server does all the work though.
	 * @return
	 * @throws IOException
	 * 				The {@link Socket} can throw this exception when the
	 * 				{@link OutputStream} is damaged (e.g. Socket closed)
	 */
	@Override
	@Command
	public String logout() throws IOException {
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);
		serverWriter.println("!logout");
		return null;
	}

	/**
	 * @brief Sends a message (chat message) to all logged-in users.
	 * @detail Just sends the command and the server does all the work though.
	 * @param message
	 * 				The message to send
	 * @return
	 * @throws IOException
	 * 				The {@link Socket} can throw this exception when the
	 * 				{@link OutputStream} is damaged (e.g. Socket closed)
	 */
	@Override
	@Command
	public String send(String message) throws IOException {
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);
		serverWriter.println("!send " + message);
		return null;
	}

	/**
	 * @brief Lists all users in alphabetical order.
	 * @detail Just sends the command and the server does all the work though.
	 * 				Uses {@link Config} file.
	 * @return
	 * @throws IOException
	 * 				The {@link Socket} can throw this exception when the
	 * 				{@link OutputStream} is damaged (e.g. Socket closed)
	 */
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

	/**
	 * @brief Sends a private message to a specified user {@arg username}
	 * @param username
	 *            User that should receive the private message
	 * @param message
	 *            Message to be sent to all online users
	 *
	 * @return Responses from the client program
	 * @throws IOException
	 * 				When the OutputStream is damaged (e.g. closed Socket) or
	 * 				the {@var secretWriter} is being closed wrong.
	 *
	 */
	@Override
	@Command
	public String msg(String username, String message) throws IOException {
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);

		serverWriter.println("!lookup " + username);
		/* blocking I/O ahead */
		String[] parts = tcpListener.getPrivateAddress().split(":");

		if (parts.length != 2) {
			return SECRADDR_MALFORMED;
		}

		Socket secretSocket = new Socket(InetAddress.getByName(parts[0]), Integer.parseInt(parts[1]));
		PrintWriter secretWriter = new PrintWriter(
				secretSocket.getOutputStream(), true);
		secretWriter.println(this.username + ": " +  message);
		BufferedReader secretReader = new BufferedReader(new InputStreamReader(secretSocket.getInputStream()));
		if (secretReader.readLine().equals("!ack")) {
			secretWriter.close();
			return username + " replied with !ack";
		}
		outputStream.println("Wrong username or user not reachable.");
		return null;
	}

	/**
	 * @brief Perfoms a lookup of given {@var username}.
	 * @detail Just sends the command and the server does all the work though.
	 * @param username
	 *            communication partner of private conversation
	 * @return
	 * @throws IOException
	 * 				The {@link Socket} can throw this exception when the
	 * 				{@link OutputStream} is damaged (e.g. Socket closed)
	 */
	@Override
	@Command
	public synchronized String lookup(String username) throws IOException {
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);

		serverWriter.println("!lookup " + username);
		return null;
	}

	/**
	 * @brief Register a address for private messaging.
	 * @param privateAddress
	 *            address consisting of 'IP:port' that is used for creating a
	 *            TCP connection
	 * @return
	 * @throws IOException
	 * 				The {@link Socket} can throw this exception when the
	 * 				{@link OutputStream} is damaged (e.g. Socket closed)
	 */
	@Override
	@Command
	public synchronized String register(String privateAddress) throws IOException {
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);
		InetAddress address;
		int port;
		/* try to parse @argument {privateAddress} into inetaddr and port */
		String[] parts = privateAddress.split(":");

		if (parts.length != 2) {
			return REGISTERADDR_MALFORMED;
		}

		serverWriter.println("!register " + privateAddress);
		address = InetAddress.getByName(parts[0]);
		port = Integer.parseInt(parts[1]);
		/* if already open close first */
		closePrivateConnection();
		/* then open a new one */
		/* backlog - requested maximum length of the queue of incoming connections. = 10 */
		privateChatServer = new ServerSocket(port, 10, address);
		privateChatServerHandler = pool.submit(new ClientPrivateListenHandler(
				privateChatServer.accept(), outputStream));
		return null;
	}

	/**
	 * @brief Prints the last public message received.
	 * @return last message | ERROR
	 */
	@Override
	@Command
	public synchronized String lastMsg() throws IOException {
		String lastMessage = tcpListener.getLastMessage();
		if (lastMessage == null) {
			return LASTMSG_EMPTY;
		}
		return lastMessage;
	}

	/**
	 * @brief Closes all client connections and exits the program
	 * @return
	 * @throws IOException
	 * 				Closing TCP/UDP can cause a problem.
	 */
	@Override
	@Command
	public String exit() throws IOException {
		shutdown = true;
		/* Close TCP connection */
		if (tcpSocket != null) {
			tcpSocket.close();
		}
		/* Close private TCP connection */
		closePrivateConnection();
		/* Close UDP connection */
		if (udpSocket != null) {
			udpSocket.close();
		}
		/* Shutdown pool */
		pool.shutdown();
		if (!pool.isShutdown()) {
			pool.shutdownNow();
			if (!pool.isShutdown()) {
				throw new RuntimeException("Pool couldn't be shut down.");
			}
		}
//		outputStream.println("Bye.");
		shell.close();
		/* return of a String not possible, shell already closed here. */
		return null;
	}

	/**
	 * @brief The program entry point
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);
		client.run();
	}

	/**
	 * @brief Close the private Socket which is created by {@method register}.
	 * @throws IOException
	 * 				Closing can cause troubles.
	 */
	private void closePrivateConnection() throws IOException {
		if (privateChatServerHandler != null && privateChatServer != null) {
			privateChatServer.close();
			if (!privateChatServer.isClosed()) {
				throw new RuntimeException("Couldn't close private TCP socket.");
			}
			privateChatServerHandler.cancel(true);
			if (!privateChatServerHandler.isCancelled()) {
				throw new RuntimeException("Couldn't close private TCP handler.");
			}
			privateChatServerHandler = null;
		}
	}

	// --- Commands needed for Lab 2. Please note that you dFo not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
