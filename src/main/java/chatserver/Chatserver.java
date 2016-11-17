package chatserver;

import java.io.*;
import java.net.*;
import java.text.Collator;
import java.util.*;
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
	private volatile HashMap<Socket, User> users;

	private ExecutorService pool;
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

		/**
		 * Creates a thread pool that creates new threads as needed, but will reuse previously
		 * constructed threads when they are available. These pools will typically improve the
		 * performance of programs that execute many short-lived asynchronous tasks.
		 *
		 * {@url https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executors.html#newCachedThreadPool()}
		 */
		this.pool = Executors.newCachedThreadPool();
		this.shell = new Shell(componentName, inputStream, outputStream);
		this.shell.register(this);
		outputStream.println(getClass().getName() + " up and Waiting for commands!");
	}

	/**
	 * @brief Creates TCP/UDP Runnables and execute them
	 * @throws RuntimeException
	 * 				We cannot handle other Exception types at runtime
	 * 				{@link SocketException}, {@link IOException}
	 */
	@Override
	public void run() {
		new Thread(shell).start();
		try {
			udpSocket = new DatagramSocket(config.getInt("udp.port"));
		} catch (SocketException e) {
			throw new RuntimeException("Cannot listen on UDP port.", e);
		}
		try {
			/* handle incoming connections from client in a separate thread */
			serverSocket = new ServerSocket(config.getInt("tcp.port"));
		} catch (IOException e) {
			throw new RuntimeException("Cannot listen on TCP port.", e);
		}
		while (!pool.isShutdown()) {
			try {
				Future a = pool.submit(new ChatServerTcpHandler(serverSocket.accept(),
						users, outputStream));
				pool.submit(new ChatServerUdpHandler(udpSocket, users));
				a.cancel(true);
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

	/**
	 * @brief Prints out the username (in alphabetical order) and login status
	 * (online/offine) about each known user
	 * @return String The list of users
	 */
	@Override
	@Command
	public synchronized String users() {
		/* Collator implements Comperator => sort alphabetical */
		Collection<String> users = new TreeSet<>(Collator.getInstance());
		for (User u : this.users.values()) {
			users.add(u.toString());
		}
		String out = "";
		int i = 1;
		for (String username: users) {
			out += (i == 1 ? "" : "\n") + i + ". " + username;
			i++;
		}
		return out;
	}

	/**
	 * @brief Shutdown the chatserver.
	 * @detail Loggs out all users, shutdown all connections and close the pool.
	 * @return String Closing message.
	 * @throws IOException
	 * 				When a {@link Socket} cannot be closed.
	 * @throws RuntimeException
 	 * 				If a {@link Socket} or the {@link ExecutorService} cannot be closed.
	 */
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
		/* return of a String not possible, shell already closed here. */
		return null;
	}

	/**
	 * @brief The program entry point
	 * @detail Starts and runs a full chatserver according to the specifications
	 * @param args The argument Array
	 */
	public static void main(String[] args) {
		Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);
		chatserver.run();
	}

}
