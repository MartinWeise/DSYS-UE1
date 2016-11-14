package client;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cli.Command;
import cli.Shell;
import util.Config;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream inputStream;
	private PrintStream outputStream;

	private Shell shell;
	private Socket tcpSocket = null;
	private DatagramSocket udpSocket = null;
	private ExecutorService pool;

	private ClientTcpListenHandler tcpRunnable;

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

		this.pool = Executors.newFixedThreadPool(10);

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
			tcpRunnable = new ClientTcpListenHandler(tcpSocket, inputStream, outputStream);
			pool.execute(tcpRunnable);
			pool.execute(new ClientUdpListenHandler(udpSocket, inputStream, outputStream));
		}
		outputStream.println(getClass().getName()
				+ " up and waiting for commands!");
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {
		// create a writer to send messages to the server
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);
		serverWriter.println("!login " + username + " " + password);
		return null;
	}

	@Override
	@Command
	public String logout() throws IOException {
		// create a writer to send messages to the server
		PrintWriter serverWriter = new PrintWriter(
				tcpSocket.getOutputStream(), true);
		serverWriter.println("!logout");
		return null;
	}

	@Override
	@Command
	public String send(String message) throws IOException {
		// create a writer to send messages to the server
		PrintWriter serverWriter = new PrintWriter(tcpSocket.getOutputStream(), true);
		serverWriter.println("!send " + message);
		return null;
	}

	@Command
	public String list() throws IOException {
		byte[] data;
		data = new String("!list").getBytes();
		DatagramPacket send = new DatagramPacket(data, data.length, InetAddress.getByName(config.getString("chatserver.host")), config.getInt("chatserver.udp.port"));
		udpSocket.send(send);
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
	@Command
	public String lastMsg() throws IOException {
		String lastMsg = tcpRunnable.getLastMsg();
		if (lastMsg == null) {
			outputStream.println("No message received!");
			return null;
		}
		outputStream.println(tcpRunnable.getLastMsg());
		return null;
	}

	@Override
	@Command
	public String exit() throws IOException {
		if (tcpSocket != null) {
			try {
				tcpSocket.close();
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

	// --- Commands needed for Lab 2. Please note that you dFo not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
