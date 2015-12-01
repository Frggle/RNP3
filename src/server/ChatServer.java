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
import java.util.concurrent.Semaphore;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * @author Marc Kaepke & Anna Steinhauer
 * @version 2.0 Ein Multi-Thread/Client Chat Raum Server (TCP Server-Socket).
 */
public class ChatServer {

    private static final int PORT = 56789; // Port, als Konstante

    private List<String> users = new ArrayList<String>(); // Liste alles
                                                          // Mitglieder im
                                                          // Chat-Raum
    private List<PrintWriter> writers = new ArrayList<PrintWriter>(); // Liste
                                                                      // aller
                                                                      // PrintWriters
                                                                      // (out to
                                                                      // client)
    private List<String> session = new ArrayList<String>(); // Liste aller
                                                            // gesendeten
                                                            // Nachrichten;
                                                            // serverseitig

    private JFrame logFrame; // Server-Log Fenster
    private JTextArea logTextArea; // Anzeigebereich des Logs
    private JScrollPane scrollPane = new JScrollPane(logTextArea);

    private Semaphore semaphore;

    /**
     * Konstruktor Erzeugt die GUI
     */
    public ChatServer(int maxThreads) {

        // Layout GUI
        logFrame = new JFrame("Chat Server - Log");
        logTextArea = new JTextArea(20, 40);
        logTextArea.setEditable(false);
        scrollPane = new JScrollPane(logTextArea);
        logFrame.getContentPane().add(scrollPane, "Center");
        logFrame.pack();
        logFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        logFrame.setVisible(true);

        semaphore = new Semaphore(maxThreads);
    }

    /**
     * Verbindet sich mit den Clients. Ein Port wird beobachtet und neue
     * Handler-Threads gestartet
     */
    private void startServer() {
        try {
            ServerSocket listener = new ServerSocket(PORT); // TCP Server Socket

            // Basis Infos werden dem Log beigefuegt
            DateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd");
            Calendar cal = Calendar.getInstance();
            logTextArea.append("Der Chat Server laeuft unter der IP: ");
            logTextArea.append(InetAddress.getLocalHost().getHostAddress() + "\n");
            logTextArea.append("---" + dateformat.format(cal.getTime()) + "---\n\n");

            /**
             * Endlosschleife und nimmt jede eingehende Verbindungsanfrage (von
             * Clients) an. Startet fuer jede Verbindung einen Thread
             * ("Handler").
             */
            Socket connectionSocket; // TCP-Standard-Socket
            try {
                while (true) {
                    try {
                        semaphore.acquire();
                        connectionSocket = listener.accept(); // "Hand-Shake"
                        new Handler(connectionSocket).start(); // Handler-Thread
                                                               // mit Socket
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            } finally {
                listener.close(); // Verbindungsabbau
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * main Methode
     */
    public static void main(String[] args) throws Exception {
        System.out.print("Chat Server wurde gestartet");

        // Erzeugt Server Instanz und startet diesen
        new ChatServer(10).startServer();
    }

    /**
     * Jeder Client besitzt seinen eigenen Handler-Thread; verwaltet die
     * Verbindung (Socket) zum Chat-Server und uebertraegt die Nachrichten.
     */
    private class Handler extends Thread {
        private String name; // Client Benutzername
        private Socket socket; // TCP-Standard-Socket
        private BufferedReader in; // Eingangsstream vom Client
        private PrintWriter out; // Ausgangsstream zum Client
        private final String PLACEHOLDER = "        ";

        /**
         * Konstruktor
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Fordert zunaechst den Nickname an, bis dieser akzeptiert wird.
         * Anschliessend wird das Protokoll in einer Dauerschleife durchlaufen
         * und kommuniziert mit dem Client.
         */
        public void run() {
            try {
                // Erzeugt den In- und Out-Stream des Clients
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                /**
                 * Server fordert zur Eingabe des Nicknames auf. Der Nickname
                 * darf im Chat-Raum noch nicht vergeben sein.
                 */
                while (true) {
                    out.println("/NAME");

                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    if (!name.startsWith("/USER")) {
                        return;
                    }
                    name = extractMessage(name);
                    writeServerLog(name, " /NAME");

                    synchronized (users) {
                        if (!users.contains(name)) {
                            users.add(name);
                            break;
                        }
                    }
                }

                // Der neue Benutzer wird "akzeptiert" -> Server schickt
                // Bestaetigung an Client
                out.println("/ACPT");
                writeServerLog(name, " /ACPT");

                // Informiert die Chat-Mitgliedern, dass ein neuer Benutzer
                // beigetreten ist
                for (PrintWriter writer : writers) {
                    writer.println("/MSGE" + PLACEHOLDER + name + " joined");
                }
                writeServerLog(name, " joined");

                synchronized (writers) {
                    writers.add(out); // Fuegt den neuen WriterStream der Liste
                                      // aller Mitglieder-Streams bei
                }

                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

                /**
                 * Nimmt die Nachrichten des Clients entgegen und antwortet bzw.
                 * verteilt die Nachrichten entsprechend -> nur an Client selbst
                 * oder an alle Clients im Chat
                 */
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                        // Wenn der Client den Chat verlassen will
                    } else if (input.equals("/QUIT" + name)) {
                        out.println("/QUIT");
                        writeServerLog(name, " disconnected");
                        synchronized (writers) {
                            for (PrintWriter writer : writers) {
                                writer.println("/MSGE" + name + " (" + sdf.format(cal.getTime()) + ") disconnected");
                            }
                        }
                    } else if (input.equals("/USRS")) {
                        synchronized (users) {
                            writeServerLog(name, input);
                            if (writers.size() == 1) {
                                out.println("/MSGEyou are alone :(");
                            } else {
                                out.println("/MSGE" + PLACEHOLDER + "list of users:");
                                String allUsers = "";
                                for (String user : users) {
                                    if (!user.equals(name)) {
                                        allUsers += "[" + user + "] ";
                                    }
                                }
                                out.println("/MSGE" + PLACEHOLDER + allUsers);
                            }
                        }
                    }
                    // Wenn eine Nachricht an alle gehen soll
                    else {
                        synchronized (writers) {
                            input = extractMessage(input);
                            writeServerLog(name, "\"" + input + "\"");
                            for (PrintWriter writer : writers) {
                                writer.println("/MSGE" + "(" + sdf.format(cal.getTime()) + ") " + name + ": " + "\"" + input + "\"");
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // Wenn der Client down ist ...
                if (name != null) {
                    users.remove(name); // entferne den Benutzer aus der
                                        // Chat-Mitglieder-Liste
                }
                if (out != null) {
                    writers.remove(out); // entferne PrintWriter vom
                                         // abgemeldeten Benutzer
                }
                try {
                    socket.close(); // Verbindungsabbau
                } catch (IOException e) {
                }
                semaphore.release();
            }
        }

        /**
         * 
         * @param message
         * @return
         */
        private String extractMessage(String message) {
            return message.substring(5);
        }

        /**
         * Schreibt die Nachricht in eine Session-ArrayList und zeigt sie
         * anschliessend im Server-Log-Fenster an. (hh:mm) <name> : <message>
         * 
         * @param name
         *            , des Users
         * @param message
         *            , die eigentliche Nachricht
         */
        private void writeServerLog(String name, String message) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

            session.add("(" + sdf.format(cal.getTime()) + ") " + name + ": " + message);
            logTextArea.append(session.get(session.size() - 1) + "\n");
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        }
    }
}
