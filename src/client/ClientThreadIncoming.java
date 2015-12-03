package client;

import java.io.BufferedReader;
import java.io.IOException;

import javax.swing.JOptionPane;

/**
 * @author Marc Kaepke & Anna Steinhauer
 * @version 2.0
 */
public class ClientThreadIncoming extends Thread {
	
	private BufferedReader in;
	private ChatClient client;
	
	/**
	 * Konstruktor
	 * @param in, BufferedReader des Client
	 * @param client, für den der Thread der ingehenden Nachrichten gestartet wird
	 */
	public ClientThreadIncoming(BufferedReader in, ChatClient client) {
		this.in = in;
		this.client = client;
	}
	
	/**
	 * Startet einen Thread, des die eingehenden Nachrichten verarbeitet.
	 */
	public void run() {
		
		while(!this.isInterrupted()) {
			String input;
			try {
				input = in.readLine();
				incomingMsg(input);
			} catch(IOException | NullPointerException e) {
			    JOptionPane.showMessageDialog(client.frame, "Der Chat-Server ist nicht mehr erreichbar.", "Achtung", JOptionPane.OK_OPTION);
                client.frame.dispose();
			} 
		}
	}
	
	/**
	 * Verarbeitet eingehende Nachrichten
	 * @param message, eingehende Nachricht
	 */
	private synchronized void incomingMsg(String message) {
		if(message.startsWith("/MSGE")) {
			message = extractMessage(message);
			client.messageArea.append(message + "\n");
			client.scrollPane.getVerticalScrollBar().setValue(client.scrollPane.getVerticalScrollBar().getMaximum());
		} else if(message.equals("/QUIT")) {
			client.logout();
		}
	}
	
	/**
	 * Methode welche die Protokollbefehle von den Nachrichten trennt
	 * @param message, welche vom Server eingeht
	 * @return nur die Nachricht ohne Protokollbefehle
	 */
	private String extractMessage(String message) {
		return message.substring(5);
	}
}
