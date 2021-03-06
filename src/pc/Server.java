package pc;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class Server extends Thread {

	public static String hostIP;
	public static ArrayList<Socket> connectedClients;
	public static ArrayList<String> connectedNames;

	public static void main(String[] args) {

		Console con = System.console();
		if(con == null) {
			try {
				String launcherName = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
				Runtime.getRuntime().exec(new String[] {"cmd", "/c", "start", "cmd", "/k", "java -jar " + launcherName});
				System.exit(0);
			} catch (IOException e) {}
		}
		
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);
		connectedClients = new ArrayList<Socket>();

		// Grab IP of server host
		hostIP = Data.grabIP();
		if ("-1".equals(hostIP)) {
			print("Could not find current host device's IP, possibly internet connection problem.");
			System.exit(-1);
		} else {
			print("Host IP is: " + hostIP);
		}

		// Start accepting clients with clientAcceptor
		clientAcceptor();
		while (true) { // This is for the server commands
			switch (sc.nextLine()) {
			case "list":
				print(connectedClients);
			}
		}
	}

	// Constantly accepts new client sockets
	public static void clientAcceptor() {
		ServerSocket serverSocket = null;
		Socket client;

		try {
			serverSocket = new ServerSocket(Data.port); // Open port in server
			print("Server online");
		} catch (IOException e1) {
			print("Error is starting server, please retry after full exit. If problem continues, port 888 is unavailable (not open to port forwarding or occupied)"); // Usually
																														// server
																														// is
																														// occupied
			System.exit(-1);
		}
		while (!serverSocket.isClosed()) { // Constantly accept new clients until server socket is closed
			try {

				client = serverSocket.accept(); // Accept method and assign the socket to client
				connectedClients.add(client);
				clientHandler(client);

			} catch (Exception e) {
				print("Failed to accept a client");
			}
		}
	}

	// This is our connection between our client, server, and eventually other
	// clients. Each time this function is called, a new thread is created
	public static void clientHandler(Socket client) {
		Thread handler = new Thread() {
			
			public boolean connected = true;
			public InputStreamReader isr;
			public BufferedReader br;
			public PrintWriter serverWriter; // This is used to write to connect to all the clients and send messages to
												// them
			public PrintWriter clientWriter; // This is used to write to the handler's respective client

			private String username;
			
			@Override
			public void run() {
				
				String intro = readClient(); // When user first connects
				username = intro.replaceAll("'", "").replaceAll(" connected", ""); // Grab their username
				//connectedNames.add(username);
				sendToAll(intro, Data.systemFont);
				
				while (connected) { // While client is still connected

					String input = readClient(); // Keeps reading and then sending to all client

					if (input == null) { // null caught from readClient means user has disconnected while trying to read input
						disconnect(0);
					} 
					else {
						sendToAll(input, Data.clientFont);
					}

				}
				// If the while loop ever stops, then it just ends
			}

			public String readClient() {
				try {
					isr = new InputStreamReader(client.getInputStream()); // Find input stream of client
					br = new BufferedReader(isr); // Buffered reader to read the input stream
					String input = br.readLine(); // Read the actual line
					return input;
				} catch (Exception e) { // This is almost always the most default way of disconnecting
					return null;
				}
			}

			public void sendToAll(String content, String font) { // Send to all Clients

				for (int i = 0; i < connectedClients.size(); i++) { // Find how many clients are connected and loops how
																	// many times
					try {
						serverWriter = new PrintWriter(connectedClients.get(i).getOutputStream());
						serverWriter.println(font + "\\" + content);
						serverWriter.flush();
					} catch (IOException ioe) {
						System.out.println("Failed to sending to " + connectedClients.get(i));
					}
				}
			}

			@SuppressWarnings("unused")
			public void sendToClient(Object object) { // Send to client privately
				try {
					clientWriter = new PrintWriter(client.getOutputStream());
					clientWriter.println(object);
					clientWriter.flush();
				} catch (IOException ioe) {
					
				}
			}

			public void disconnect(int status) {
				connected = false;
				connectedClients.removeAll(Collections.singleton(client));
				//connectedNames.removeAll(Collections.singleton(username));
				try {
					client.close();
				} catch (IOException e) {

				}
				if (status == 0) { // Default exit
					sendToAll(username + " has disconnected", Data.systemFont);
				} else if (status == -1) {
					sendToAll(username + " has disconnected (Error)", Data.systemErrorFont);
				}

			}
		};

		// In case client forcefully disconnected such as closing window
		handler.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {

			}
		});

		handler.start();
	}

	public static void print(Object object) {
		System.out.println(object);
	}
}
