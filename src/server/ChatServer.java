package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * ChatServer.java 
 * Version: 2.0 
 * Autor: Marc Kaepke & Anna Steinhauer 
 * Zweck: Ein multi-thread/client Chat Raum Server (TCP Server-Socket).
 */
public class ChatServer {
	
	private static final int PORT = 56789; // Port, als Konstante
	
	private List<String> users = new ArrayList<String>();	// Liste alles Mitglieder im Chat-Raum
	private List<PrintWriter> writers = new ArrayList<PrintWriter>();	// Liste aller PrintWriters (out to client)
	private List<String> session = new ArrayList<String>();	// Liste aller gesendeten Nachrichten; serverseitig
	
	private JFrame logFrame;	// Server-Log Fenster
	private JTextArea logTextArea;	// Anzeigebereich des Logs
	private JScrollPane scrollPane = new JScrollPane(logTextArea);
	
	/**
	 * Konstruktor 
	 * Erzeugt die GUI
	 */
	public ChatServer() {
		
		// Layout GUI
		logFrame = new JFrame("Chat Server - Log");
		logTextArea = new JTextArea(20, 40);
		logTextArea.setEditable(false);
		scrollPane = new JScrollPane(logTextArea);
		logFrame.getContentPane().add(scrollPane, "Center");
		logFrame.pack();
		logFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		logFrame.setVisible(true);
		
	}
	
	/**
	 * Verbindet sich mit den Clients.
	 * Ein Port wird beobachtet und neue Handler-Threads gestartet
	 */
	private void startServer() {
		try {
			ServerSocket listener = new ServerSocket(PORT);	// TCP Server Socket
			
			// Basis Infos werden dem Log beigefuegt
			DateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd");
			Calendar cal = Calendar.getInstance();
			logTextArea.append("Der Chat Server laeuft unter der IP: ");
			logTextArea.append(InetAddress.getLocalHost().getHostAddress() + "\n");
			logTextArea.append("---" + dateformat.format(cal.getTime()) + "---\n\n");
			
			/**
			 * Endlosschleife und nimmt jede eingehende Verbindungsanfrage (von Clients) an. Startet fuer jede
			 * Verbindung einen Thread ("Handler").
			 */
			Socket connectionSocket;	// TCP-Standard-Socket
			try {
				while(true) {
					connectionSocket = listener.accept();	// "Hand-Shake"
					new Handler(connectionSocket).start();	// Handler-Thread mit Socket
				}
			} finally {
				listener.close();	// Verbindungsabbau
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * main Methode
	 */
	public static void main(String[] args) throws Exception {
		System.out.print("Chat Server wurde gestartet");
		
		// Erzeugt Server Instanz und startet diesen
		new ChatServer().startServer();
	}
	
	/**
	 * Jeder Client besitzt seinen eigenen Handler-Thread; verwaltet die Verbindung (Socket) zum Chat-Server und
	 * uebertraegt die Nachrichten.
	 */
	private class Handler extends Thread {
		private String name;	// Client Benutzername
		private Socket socket;	// TCP-Standard-Socket
		private BufferedReader in;	// Eingangsstream vom Client
		private PrintWriter out;	// Ausgangsstream zum Client
		private final String PLACEHOLDER = "        ";
		
		/**
		 * Konstruktor
		 */
		public Handler(Socket socket) {
			this.socket = socket;
		}
		
		/**
		 * Fordert zunaechst den Nickname an, bis dieser akzeptiert wird. Anschliessend wird das Protokoll in einer
		 * Dauerschleife durchlaufen und kommuniziert mit dem Client.
		 */
		public void run() {
			try {
				// Erzeugt den In- und Out-Stream des Clients
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				
				/**
				 * Server fordert zur Eingabe des Nicknames auf. Der Nickname darf im Chat-Raum noch nicht vergeben
				 * sein.
				 */
				while(true) {
					out.println("/NME");
					
					name = in.readLine();
					if(name == null) {
						return;
					}
					if(!name.startsWith("/USR")) {
						return;
					}
					name = extractMessage(name);
					writeServerLog(PLACEHOLDER, name + " /NME");
					
					synchronized(users) {
						if(!users.contains(name)) {
							users.add(name);
							break;
						}
					}
				}
				
				// Der neue Benutzer wird "akzeptiert" -> Server schickt Bestaetigung an Client
				out.println("/NOK");
				writeServerLog(PLACEHOLDER, name + " /NOK");
				
				// Informiert die Chat-Mitgliedern, dass ein neuer Benutzer beigetreten ist
				for(PrintWriter writer : writers) {
					writer.println("/MSG" + PLACEHOLDER + name + " joined");
					writer.println("/USR" + name);
				}
				writeServerLog(PLACEHOLDER, name + " joined");
				
				synchronized(writers) {
					writers.add(out); // Fuegt den neuen WriterStream der Liste aller Mitglieder-Streams bei	
				}
				
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
				
				/**
				 * Nimmt die Nachrichten des Clients entgegen und antwortet bzw. verteilt die Nachrichten entsprechend
				 * -> nur an Client selbst oder an alle Clients im Chat
				 */
				while(true) {
					String input = in.readLine();
					if(input == null) {
						return;
						// Wenn der Client den Chat verlassen will
					} else if(input.equals("/QIT" + name)) {
						out.println("/QIT");
						writeServerLog(PLACEHOLDER, name + " disconnected");
						synchronized(writers) {
							for(PrintWriter writer : writers) {
								writer.println("/MSG" + name + " (" + sdf.format(cal.getTime()) + ") disconnected");
								writer.println("/USR" + name);
							}	
						}
					} else if(input.equals("/USR")) {
						synchronized(users) {
							String res = "";
							for(String user : users) {
								res += (user + "\n");
							}
							out.println("/USR" + res);
						}
					}
					// Wenn eine Nachricht an alle gehen soll
					else {
						synchronized(writers) {
							input = extractMessage(input);
							writeServerLog(name, input);
							for(PrintWriter writer : writers) {
								writer.println("/MSG" + name + " (" + sdf.format(cal.getTime()) + ") : " + input);
							}
						}
					}
				}
			} catch(IOException e) {
				System.out.println(e);
			} finally {
				// Wenn der Client down ist ...
				if(name != null) {
					users.remove(name);		// entferne den Benutzer aus der Chat-Mitglieder-Liste
				}
				if(out != null) {
					writers.remove(out);	// entferne PrintWriter vom abgemeldeten Benutzer
				}
				try {
					socket.close();	// Verbindungsabbau
				} catch(IOException e) {
				}
			}
		}
		
		/**
		 * 
		 * @param message
		 * @return
		 */
		private String extractMessage(String message) {
			return message.substring(4);
		}
		
		/**
		 * Schreibt die Nachricht in eine Session-ArrayList und zeigt sie anschliessend im Server-Log-Fenster an.
		 * <beforeDate> (hh:mm) : <afterDate>
		 * @param beforeDate, String wird vor der Uhrzeit eingefuegt
		 * @param afterDate, String wird nach der Uhrzeit eingefuegt
		 */
		private void writeServerLog(String beforeDate, String afterDate) {
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
			
			session.add(beforeDate + " (" + sdf.format(cal.getTime()) + ") : " + afterDate);
			logTextArea.append(session.get(session.size() - 1) + "\n");
			scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
		}
	}
}
