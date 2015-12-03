package praktikum;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.sound.midi.Synthesizer;

public class ChatThreadOut extends Thread {

	String msg = "";
	ChatClient_praktikum client;
	
    public ChatThreadOut(ChatClient_praktikum client, String msg) {
       this.client = client;
       this.msg = msg;
    }

    /**
     * Geht alle Nutzer in der Liste durch und schickt an deren Adressen ein UDP Paket.
     */
    public void run() {
    	
    	
        DatagramSocket socket = null;       //udp socket
        byte[] buf = new byte[1024];        //byte buffer
        buf = msg.getBytes();     //fülle buffer mit eingabe

        for (String address: client.userList.keySet()) {
            try {
                socket = new DatagramSocket();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(address), client.PORT_UDP);     //udp packet erzeugen
                socket.send(packet);        //sende an jeden user
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
