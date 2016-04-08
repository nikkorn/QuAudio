package com.quaudio.quserver.clientmanager;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientManagerUT {
	private int testClientManagerConnectionPort = 50607;
	private int testTimeout = 4000;
	
	@Test
	public void connectToClientManager() {
		// Set up the ClientManager.
		ClientManager clientManager = new ClientManager(testClientManagerConnectionPort);
		clientManager.start();
		// Construct our request.
		JSONObject connectionRequest = new JSONObject();
		connectionRequest.put("client_id", "test_id");
		// Connect to the ClientManager.
		Socket clientSocket = null;
		try {
			clientSocket = new Socket();
			clientSocket.connect(new InetSocketAddress("127.0.0.1", testClientManagerConnectionPort), testTimeout);
			BufferedReader responseReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 
			PrintWriter responseWriter = new PrintWriter(clientSocket.getOutputStream());
			// Send our request.
			responseWriter.println(connectionRequest.toString());
			responseWriter.flush();
			// Get the response from the ClientManager.
			String response = responseReader.readLine();
			// Ensure that we got the 'ACCEPTED' response.
			assertTrue("failed to get the expected 'ACCEPTED' response from ClientManager", response.trim().equals("ACCEPTED"));
		} catch (SocketTimeoutException e) {
			fail("got socket timeout during connection to ClientManager");
		} catch (IOException e) {
			fail("got IOException during connection to ClientManager");
		} finally {
			if(clientSocket != null) {
				try {
					clientSocket.close();
				} catch (IOException e) {}
			}
		}
	}
}
