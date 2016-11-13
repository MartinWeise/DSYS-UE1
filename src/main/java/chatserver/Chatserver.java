package chatserver;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cli.Command;
import cli.Shell;
import util.Config;
import util.Log;
import util.User;

public class Chatserver implements IChatserverCli, Runnable {

	private Config config;
	private InputStream inputStream;
	private PrintStream outputStream;

	private Shell shell;
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
		this.config = config;
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.users = new HashMap<>();
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
	@Command
	public String users() throws IOException {
		// TODO Auto-generated method stub
		String out = "";
		int i = 1;
		for (User u : users.values()) {
			out += (i == 1 ? "" : "\n") + i + ". " + u;
			i++;
		}
		outputStream.println(out);
		return null;
	}

	@Override
	@Command
	public String exit() throws IOException {
		serverSocket.close();
		// {@url: https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html}
		pool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(1, TimeUnit.SECONDS))
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

}
