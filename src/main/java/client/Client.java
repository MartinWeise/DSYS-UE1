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

	private Future tcpSubmit;
	private Future udpSubmit;

	private final String LASTMSG_EMPTY = "No message received!";

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

		this.pool = Executors.newFixedThreadPool(2);

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
			tcpSubmit = pool.submit(new ClientTcpListenHandler(tcpSocket, inputStream, outputStream));
			udpSubmit = pool.submit(new ClientUdpListenHandler(udpSocket, inputStream, outputStream));
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
		return null;
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

		System.out.println(">> " + address);
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
		if (tcpSocket != null) {
//			try {
				/* now try to close the TCP listener */
				tcpSubmit.cancel(true);
				udpSubmit.cancel(true);
				if (!tcpSubmit.isCancelled()) {
					throw new RuntimeException("TCP Listener Thread couldn't be closed.");
				}
			outputStream.close();
			inputStream.close();
				if (!pool.isShutdown()) {
					pool.shutdown();
					if (!pool.isShutdown()) {
						throw new RuntimeException("Pool couldn't be closed.");
					}
				}
//			} catch (IOException e) {
//				throw new RuntimeException("exit", e);
//			}
		}
		this.shell.close();
		// TODO not working when not logged in first
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

	// --- Commands needed for Lab 2. Please note that you dFo not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
