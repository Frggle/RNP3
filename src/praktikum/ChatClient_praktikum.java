package praktikum;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.sun.xml.internal.bind.api.impl.NameConverter.Standard;

/**
 * @author Marc Kaepke & Anna Steinhauer
 * @version 2.0 Ein TCP-Client verbindet sich, mit anderen Clients, mit einem
 *          Chat-Server. Der Client folgt dabei dem Chat-Protokoll (NAME, ACPT,
 *          MSGE und QUIT). Der Client verfuegt ueber eine grafische
 *          Swing-Oberflaeche und zeigt den Chat-Verlauf an.
 */
public class ChatClient_praktikum implements ActionListener{

	BufferedReader in; // Eingabestream vom Server
	PrintWriter out; // Ausgabestream zum Server

	JFrame frame = new JFrame("HAW - RN"); // Chat-Fenster
	JTextField textField = new JTextField(40); // Eingabezeile
	public JTextArea messageArea = new JTextArea(8, 40); // Ausgabebereich von allen
													// Chat-Mitgliedern
	JScrollPane scrollPane = new JScrollPane(messageArea);

	String user = "anonym"; // Initial Nickname

	String hostname; // Hostname
	final int PORT_TCP = 60000;
	final int PORT_UDP = 60001;
	final int BUFFER_SIZE = 1024;

	InetAddress serverIP;

	ClientThreadIn clientThreadIn = null;
	Socket socket = null;
	DatagramSocket clientSocket = null;

	Map<String, String> userList;

	/**
	 * Konstruktor
	 */
	public ChatClient_praktikum() {
		userList = new HashMap<>();
		builtGUI();
	}

	/**
	 * Erzeugt die GUI eines Clients und setzt Listeners.
	 */
	private void builtGUI() {
		// Layout GUI
		textField.setEditable(false); // wird erst nach erfolgreichem Anmelden
										// am Chat-Server aktiviert
		messageArea.setEditable(false); // Nachrichten-Bereicha kann nicht
										// veraendert werden
		frame.getContentPane().add(textField, BorderLayout.SOUTH);
		frame.getContentPane().add(scrollPane, "Center");
		frame.pack();

		// Listener auf das Beenden/ Schliessen des Fensters
		frame.addWindowListener(new WindowAdapter() {
			/*
			 * Sobald das Fenster geschlossen wird, ohne dass der Benutzer sich
			 * abgemeldet hat, wird der Chat-Server ueber den Befehl "/quit"
			 * informiert, dass der Benutzer den Chat verlassen hat.
			 */
			@Override
			public void windowClosing(WindowEvent e) {
				out.println("LOGOT");
				logout("You left the chat");
				System.exit(0);
			}

			@Override
			public void windowClosed(WindowEvent e) {
				out.println("LOGOT");
				logout("You left the chat");
				System.exit(0);
			}
		});

		// Listener auf der Eingabezeile
		textField.addActionListener(this);
	}

	private void writeToServer(String msg) {
		byte[] sendData = null;
		try {
			sendData = msg.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			System.err.println("Error bei UTF-8");
		}

		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, PORT_UDP);

		try {
			clientSocket.send(sendPacket);
		} catch (IOException e) {
			System.err.println("Error beim senden");
		}
	}

	/**
	 * Erstellt die Verbindung zum Server
	 */
	private void connectToServer() {
		String serverAddress = null;
		boolean isConnected = false;

		while (!isConnected) {
			try {
				serverAddress = getServerAddress();
				serverIP = InetAddress.getByName(serverAddress);
				socket = new Socket(serverAddress, PORT_TCP); // Verbindungsaufbau
																// zum Server
				clientSocket = new DatagramSocket();
				isConnected = true;
			} catch (NoRouteToHostException e) {
				System.err.println(e.toString());
				isConnected = false;
			} catch (UnknownHostException e2) {
				System.err.println(e2.toString());
				isConnected = false;
			} catch (ConnectException e3) {
				System.err.println(e3.toString());
				isConnected = false;
			} catch (IOException e4) {
				System.err.println(e4.toString());
				isConnected = false;
			}
		}

		// Initialisiere In- und Out-Stream
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			out = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			System.err.println(e.toString());
		}

		login();
	}

	/**
	 * Log in des Users auf dem Server, bei vergebenem Usernamen wird solange
	 * nach Usernamen gefragt, bis ein nicht vergebener Username eingegeben
	 * wird.
	 */
	private void login() {
		try {
			boolean loggedIn = false;

			while (!loggedIn) {
				user = getName();
				out.println("LOGIN " + user);

				String input = in.readLine();

				if (input.equals("ACK")) {
					loggedIn = true;
					frame.setTitle(frame.getTitle() + " as " + user); // passt
																		// Titel
																		// vom
																		// Client
																		// Fenster
																		// an
					textField.setEditable(true); // "aktiviert" das Eingabefeld
					textField.requestFocusInWindow(); // setzt den Fokus auf das
														// Eingabefeld
				} else {
					logout("falscher Username");
				}
			}

			UserListThread userListThread = new UserListThread(this);
			userListThread.start();

			System.err.println(userList.toString());

			messageArea.append("BLAA\n");
			
			clientThreadIn = new ClientThreadIn(this);
			clientThreadIn.start();
		} catch (IOException e) {
			System.err.println("failed to login!");
		}
	}

	/**
	 * Log out vom Server. Es wird ein Info-Fenster angezeigt, dass man den Chat
	 * verlassen hat.
	 */
	protected void logout(String msg) {
		JOptionPane.showMessageDialog(frame, msg, "Good bye", JOptionPane.OK_OPTION);
		frame.dispose();

		clientThreadIn.interrupt();

		try {
			out.close();
			in.close();
			socket.close();
			clientSocket.close();
		} catch (IOException e) {
			System.err.println("failed to logout!");
		}
	}

	/**
	 * Erzeugt ein InputDialog und fordert zur Eingabe des Hostnames auf.
	 * 
	 * @return Hostname des zu verbindenen Servers
	 */
	private String getServerAddress() {
		do {
			hostname = JOptionPane.showInputDialog(frame, "IP Adresse vom Chat-Server:", "RNP2",
					JOptionPane.QUESTION_MESSAGE);
		} while (hostname.isEmpty()); // wiederholt bis Hostname gefuellt ist
		return hostname;
	}

	/**
	 * Erzeugt ein InputDialog und fordert zur Eingabe des Nicknames auf.
	 * 
	 * @return Nickname des Benutzers
	 */
	private String getName() {
		do {
			user = JOptionPane.showInputDialog(frame, "Nickname angeben:", "Nickname", JOptionPane.PLAIN_MESSAGE);
		} while (user.isEmpty()); // wiederholt bis Nickname gefuellt ist
		return user;
	}

	/**
	 * Verbindet sich mit dem Chat-Server und tritt der Prozess-Schleife bei.
	 */
	public void startClient() throws IOException {

		connectToServer();
	}

	/**
	 * Startet einen Client
	 */
	public static void main(String[] args) throws Exception {
		ChatClient_praktikum client = new ChatClient_praktikum();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setLocationRelativeTo(null); // positioniert das Fenster
													// mittig
		client.frame.setVisible(true); // macht es sichtbar
		client.startClient(); // startet den Client
	}

	/*
	 * Sobald die ENTER-Taste gedrueckt wurde, wird der Inhalt an den
	 * Chat-Server geschickt. Anschließend wird die Eingabezeile
	 * entleert/ geloescht
	 */
	public void actionPerformed(ActionEvent e) {

		String message = textField.getText();
		if (message.toUpperCase().equals("USERS")) {
			// out.println("/USRS");
			textField.setText("");

			// TODO

		} else if (message.toUpperCase().equals("QUIT")) {
			// out.println("/QUIT" + user);
			writeToServer("LOGOT");
			textField.setText("");
			logout("You left the chat");
		} else {
			ChatThreadOut chatOut = new ChatThreadOut(this, message);
			chatOut.start();
			textField.setText("");
		}
	}
}
