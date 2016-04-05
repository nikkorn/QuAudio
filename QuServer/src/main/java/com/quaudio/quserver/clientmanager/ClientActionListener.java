package com.quaudio.quserver.clientmanager;

import java.io.BufferedReader;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import com.quaudio.quserver.clientmanager.IncomingAction;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class ClientActionListener implements Runnable {
	// The BufferedReader with which we are reading JSON representing IncomingActions from the client.
	private BufferedReader actionReader;
	// Is the connection alive?
	private volatile boolean isConnected = true;
	// A reference to the actual client
	private Client client;
	
	public ClientActionListener(Client client, BufferedReader actionReader) {
		this.actionReader = actionReader;
		this.client = client;
	}

	@Override
	public void run() {
		// Listen for client IncomingAction JSON forever
		while(true) {
			try {
				// Wait for input from the client
				String rawIncomingActionJSON = actionReader.readLine();
				
				// Attempt to construct a IncomingAction JSON object using the raw input.
				JSONObject incomingActionJSONObject = null;
				try {
					if(rawIncomingActionJSON != null) {
						// Attempt to create a JSONObject using the read input
						incomingActionJSONObject = new JSONObject(rawIncomingActionJSON);
						// We were able to create a valid JSONObject, now make our IncomingAction
						IncomingAction newIncomingAction = new IncomingAction(incomingActionJSONObject);
						// Pass the new IncomingAction to the ActionChannel to be processed at a later time
						client.queueIncomingAction(newIncomingAction);
					} else {
						// We shouldn't have got a null value from our BufferedReader, we must have disconnected
						throw new IOException("client disconnected");
					}
				} catch (JSONException e) {
					// The raw JSON string that the client has sent us doesn't seem to be valid JSON.
					// Just do nothing and continue listening.
				}
			} catch(IOException e) {
				// We have had a connection error, mark this listener as disconnected
				isConnected = false;
				break;
			}
		}
	}
	
	public boolean isConnected() {
		return isConnected;
	}
}
