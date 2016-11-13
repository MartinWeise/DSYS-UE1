package chatserver;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import util.Config;
import util.Log;
import util.User;

public class Chatserver implements IChatserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream inputStream;
	private PrintStream outputStream;

	protected boolean isStopped = false;
	private ServerSocket serverSocket;
	private DatagramSocket udpSocket;
	private HashMap<Socket, User> users;
	private ExecutorService pool;

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
	public Chatserver(String componentName, Config config,
			InputStream inputStream, PrintStream outputStream) {
		this.componentName = componentName;
		this.config = config;
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.users = new HashMap<>();
		this.pool = Executors.newFixedThreadPool(10);
	}

	@Override
	public void run() {
		// create and start a new TCP ServerSocket
		try {
			udpSocket = new DatagramSocket(config.getInt("udp.port"));
		} catch (SocketException e) {
			throw new RuntimeException("Cannot listen on UDP port.", e);
		}
		try {
			// handle incoming connections from client in a separate thread
			serverSocket = new ServerSocket(config.getInt("tcp.port"));
		} catch (IOException e) {
			throw new RuntimeException("Cannot listen on TCP port.", e);
		}
		outputStream.println("Server is up! Hit <ENTER> to exit!");
//		BufferedReader reader = new BufferedReader(new InputStreamReader(
//				System.in));
//		try {
//			System.out.println(reader.readLine());
//		} catch (IOException e) {
//			// IOException from System.in is very very unlikely (or impossible)
//			// and cannot be handled
//			throw new RuntimeException("readLine");
//		}
		while (!pool.isShutdown()) {
			try {
				pool.execute(new ChatServerTcpHandler(serverSocket.accept(), users, inputStream, outputStream));
				pool.execute(new ChatServerUdpHandler(udpSocket, users, inputStream, outputStream));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			exit();
		} catch(IOException e) {
			new Log("Cannot close server.");
			throw new RuntimeException("Cannot close server.", e);
		}
	}

	@Override
	public String users() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String exit() throws IOException {
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		}
		// {@url: https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html}
		pool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
		Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);
		// TODO: start the chatserver
		chatserver.run();
	}

	protected synchronized boolean isStopped() {
		return isStopped;
	}

}
