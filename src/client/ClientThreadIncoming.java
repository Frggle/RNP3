package client;

import java.io.BufferedReader;
import java.io.IOException;

import javax.swing.JOptionPane;

public class ClientThreadIncoming extends Thread {
	
	private BufferedReader in;
	private ChatClient client;
	
	/**
	 * @param in
	 * @param client
	 */
	public ClientThreadIncoming(BufferedReader in, ChatClient client) {
		this.in = in;
		this.client = client;
	}
	
	/**
	 * 
	 */
	public void run() {
		
		while(!this.isInterrupted()) {
			String input;
			try {
				input = in.readLine();
				incomingMsg(input);
			} catch(IOException e) {
			    JOptionPane.showMessageDialog(client.frame, "Der Chat-Server ist nicht mehr erreichbar.", "Achtung", JOptionPane.OK_OPTION);
                client.frame.dispose();
			} 
		}
	}
	
	/**
	 * @param message
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
	 * 
	 * @param message
	 * @return
	 */
	private String extractMessage(String message) {
		return message.substring(5);
	}
}
