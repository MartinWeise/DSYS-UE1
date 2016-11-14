package chatserver;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.*;

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
	private LinkedList<Future> submits;
	private boolean shutdown = false;

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

		/*
		 * Creates a thread pool that creates new threads as needed, but will reuse previously
		 * constructed threads when they are available. These pools will typically improve the
		 * performance of programs that execute many short-lived asynchronous tasks.
		 * @url{https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executors.html#newCachedThreadPool()}
		 */
		this.pool = Executors.newCachedThreadPool();
		this.submits = new LinkedList<>();

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
				submits.add(pool.submit(new ChatServerTcpHandler(serverSocket.accept(), users, inputStream, outputStream)));
				submits.add(pool.submit(new ChatServerUdpHandler(udpSocket, users, inputStream, outputStream)));
			} catch (IOException e) {
				if (!shutdown) {
					throw new RuntimeException("pool submit", e);
				}
			}
		}
		try {
			exit();
		} catch(IOException e) {
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
		shutdown = true;
		/* Logout each active user */
		for (Socket s : users.keySet()) {
			if (!s.isClosed()) {
				s.close();
				if (!s.isClosed()) {
					throw new RuntimeException("Thread couldn't be closed.");
				}
			}
		}
		/* Shutdown TCP Socket */
		serverSocket.close();
		/* Shutdown UDP Socket */
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
		return "Shut down completed! Bye ..";
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
