package praktikum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserListThread extends Thread {

	BufferedReader in;
	PrintWriter out;
	Map<String, String> userList;
	ChatClient_praktikum client;
	
	public UserListThread(ChatClient_praktikum client) {
		this.in = client.in;
		this.out = client.out;
		this.client = client;
		userList = new ConcurrentHashMap<>();
	}
	
	@Override
	public void run() {
		
		while(!this.isInterrupted()){
			out.println("USERS");
			
			String input = null;
			try {
				input = in.readLine();
				System.err.println(input);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(input.startsWith("USERS")) {
				input = input.substring(6);
				if(input.length() > 1) {
					String[] inputArray = input.split(" ");
					for(int i = 1; i < inputArray.length; i += 2) {
						userList.put(inputArray[i - 1], inputArray[i]);
					}
					
					synchronized (client.userList) {
						client.userList.clear();
						client.userList.putAll(userList);
					}
				}
			}
			
			
			try {
				sleep(2000);
			} catch (InterruptedException e) {
				System.err.println("Error bei Userlist");
			}
		}
	}
}
