package praktikum;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;

/**
 * @author Marc Kaepke & Anna Steinhauer
 * @version 2.0
 */
public class ClientThreadIn extends Thread {

	private ChatClient_praktikum client;
	private final int BUFFER_SIZE = 30;
	private String ip = null;

	/**
	 * Konstruktor
	 * 
	 * @param in,
	 *            BufferedReader des Client
	 * @param client,
	 *            für den der Thread der ingehenden Nachrichten gestartet wird
	 */
	public ClientThreadIn(ChatClient_praktikum client) {
		this.client = client;
	}

	/**
	 * Startet einen Thread, des die eingehenden Nachrichten verarbeitet.
	 */
	public void run() {
		DatagramSocket socket = null; // udp socket
		byte[] buf = new byte[1024]; // byte buffer
		DatagramPacket packet = new DatagramPacket(buf, buf.length); // udp
																		// socket
		try {
			socket = new DatagramSocket(60001);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		while (true) {
			try {
				socket.receive(packet); // empfange packet
			} catch (IOException e) {
				e.printStackTrace();
			}
			String msg = new String(packet.getData(), StandardCharsets.UTF_8);
			ip = packet.getAddress().getHostAddress();
			
			String msg2 = msg.trim();

			String user = client.userList.get(ip);
			System.err.println(user + ":" + msg2.length());
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					client.messageArea.append(user + ":" + msg2 + "\n");
				}
			});

		}
	}

}
