package com.quaudio.quserver.clientmanager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import com.quaudio.quserver.server.Log;
import com.quaudio.quserver.server.Server;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class ClientManager implements Runnable {
	private ServerSocket clientManagerServerSocket = null;
	private HashMap<String, Client> clients = new HashMap<String, Client>();
	private Object newcomersBoolLock = new Object();
	private volatile boolean hasNewcomers = false;
	
	public ClientManager(int port) {
		try {
			clientManagerServerSocket = new ServerSocket(port);
		} catch (IOException e) {
			// If this fails they quit the app.
			Log.log(Log.MessageType.CRITICAL, "CLIENT_MANAGER", "failed to initialise ClientManager server socket");
			Log.forceWrite();
			System.exit(0);
		}
	}
	
	/**
	 * Carry out processing on client map.
	 * @param server 
	 */
	public void process(Server server) {
		synchronized(clients) {
			// Remove any disconnected clients from map and allow each client object
			// to send any OutgoingActions that it has been queueing
			Iterator<Map.Entry<String,Client>> iter = clients.entrySet().iterator();
			while (iter.hasNext()) {
			    Map.Entry<String,Client> entry = iter.next();
			    if(!entry.getValue().isConnected()){
			    	// Remove if disconnected.
			    	Log.log(Log.MessageType.INFO, "CLIENT_MANAGER", "client '" + entry.getKey() + "' disconnected");
			        iter.remove();
			    } else {
			    	// Client is still connected, now is a good time to allow the client 
			    	// object to fire off its queued OutgoingActions.
			    	entry.getValue().sendPendingOutgoingActions();
			    }
			}
		}
	}
	
	public void start() {
		// The ClientManager needs to be started on its own thread.
		Thread clientManagerThread = new Thread(this);
		clientManagerThread.setDaemon(true);
		clientManagerThread.start();
	}

	@Override
	public void run() {
		// Forever listen for and handle incoming connection requests
		while(true) {
			boolean gotSocket = true;
			Socket senderSocket = null;
			try {
				senderSocket = clientManagerServerSocket.accept();
			} catch (IOException e) {
				// If we get an IOException here then just log it as an unsuccesful connection.
				gotSocket = false;
				Log.log(Log.MessageType.WARNING, "CLIENT_MANAGER", "failed to get connecting client socket");
			}
			// Only continue if we successfully grabbed a Socket.
			if(gotSocket) {
				Log.log(Log.MessageType.INFO, "CLIENT_MANAGER", "got connection request from client at '" + senderSocket.getRemoteSocketAddress().toString() + "'");
		
				// Get the clients request JSON and determine if they will be added as a new Client
				BufferedReader connectionRequestJSONReader = null;
				try {
					connectionRequestJSONReader = new BufferedReader(new InputStreamReader(senderSocket.getInputStream()));
				} catch (IOException e) {
					// Failed to set up reader from resquest JSON
					Log.log(Log.MessageType.WARNING, "CLIENT_MANAGER", "failed to set up JSON request reader");
					// Continue and begin listening for new connection request.
					continue;
				}
				
				// We are expecting the client to be sending a connection request as a JSON string.
				// Read it from the socket and process it.
				String rawRequestJSON = null;
				try {
					rawRequestJSON = connectionRequestJSONReader.readLine();
				} catch (IOException e) {
					// Failed to read resquest JSON
					Log.log(Log.MessageType.WARNING, "CLIENT_MANAGER", "failed to read resquest JSON");
					// Continue and begin listening for new connection request.
					continue;
				}
				
				// Convert raw request JSON to a JSON object
				JSONObject requestJSON = null;
				try {
					requestJSON = new JSONObject(rawRequestJSON);
				} catch (JSONException jsonException) {
					// The raw request JSON is invalid
					Log.log(Log.MessageType.WARNING, "CLIENT_MANAGER", "the raw request JSON is invalid");
					// Continue and begin listening for new connection request.
					continue;
				}
				
				// We have our JSON request object, now determine whether we will be accepting the requester as a new client.
				processConnectionRequest(requestJSON, senderSocket, connectionRequestJSONReader);
			}
		}
	}
	
	/**
	 * Processes a connection request and sends a response to the client, returns true if connection is accepted.
	 * @param requestJSON
	 * @param senderSocket
	 * @return
	 */
	public void processConnectionRequest(JSONObject requestJSON, Socket senderSocket, BufferedReader reader) {
		PrintWriter responseWriter = null;

		try {
			responseWriter = new PrintWriter(senderSocket.getOutputStream());
		} catch (IOException e) {
			// If we weren't able to to create our PrintWriter then just deny the connection request
			Log.log(Log.MessageType.WARNING, "CLIENT_MANAGER", "unable to initialise response PrintWriter");
			return;
		}
		// Firstly, check if the client is already present in the Client map
		// If so then respond with CLIENT_ALREADY_CONNECTED
		boolean clientConnected = false;
		synchronized(clients) {
			if(clients.containsKey(requestJSON.getString("client_id"))) {
				clientConnected = true;
			}
		}
		// Is the client already connected?
		if(clientConnected) {
			// This client is already connected, send the appropriate response to the client
			responseWriter.println("CLIENT_ALREADY_CONNECTED");
			responseWriter.flush();
			Log.log(Log.MessageType.WARNING, "CLIENT_MANAGER", "client is already connected");
			return;
		}
		
		// Next we need to check that the requester has supplied the correct access password (if we require one)
		// Check that the server properties file has been initialised.
		if(Server.properties != null) {
			String accessPassword = Server.properties.getAccessPassword();
			if(accessPassword != "" && !requestJSON.getString("access_password").equals(accessPassword)) {
				// An access password is required, but the client has supplied an incorrect one, notify the user and decline
				responseWriter.println("WRONG_ACCESS_PASSWORD");
				responseWriter.flush();
				Log.log(Log.MessageType.WARNING, "CLIENT_MANAGER", "client supplied incorrect access password");
				return;
			}
		} else {
			// Properties have not been properly initialised, this may be due to this method being
			// run in the context of a unit/integration test.
			Log.log(Log.MessageType.WARNING, "CLIENT_MANAGER", "Server properties not initalised, cannot get access password");
		}
		
		// It seems that we can't find a reason to decline this request, accept and notify the client
		responseWriter.println("ACCEPTED");
		responseWriter.flush();
		Log.log(Log.MessageType.INFO, "CLIENT_MANAGER", "accepted client '" + requestJSON.getString("client_id")  + "'");
		
		// Initialise a new client
		Client acceptedClient = new Client(reader, responseWriter);
		
		// Add the client to our clients list.
		synchronized(clients) {
			clients.put(requestJSON.getString("client_id"), acceptedClient);
		}
		
		// Set a flag to show that the ClientManager has a new client. This indicates that important 
		// information should be re-broadcast
		synchronized(newcomersBoolLock) {
			hasNewcomers = true;
		}
	}
	
	/**
	 * Returns true if the ClientManager has added any new clients since the last time hasNewClients() was called.
	 * @return
	 */
	public boolean hasNewClients() {
		boolean hasNewClients = false;
		synchronized(newcomersBoolLock) {
			hasNewClients = this.hasNewcomers;
			this.hasNewcomers = false;
		}
		return hasNewClients;
	}
	
	/**
	 * Queue an OutgoingAction to be sent to all connected clients.
	 * @param outgoingAction
	 */
	public void queueOutgoingAction(OutgoingAction outgoingAction) {
		synchronized(clients) {
			for(Client client : clients.values()) {
				client.queuePendingOutgoingAction(outgoingAction);
			}
		}
	}
	
	/**
	 * Sends a welcome package of important OutgoingActions to newly connected clients.
	 * @param outgoingActions
	 */
	public void sendWelcomePackages(ArrayList<OutgoingAction> outgoingActions) {
		synchronized(clients) {
			for(Client client : clients.values()) {
				// Only send the package if the client is newly connected.
				if(client.isNewClient()) {
					// Queue each OutgoingAction that we need to send to the new client
					for(OutgoingAction outgoingAction : outgoingActions) {
						client.queuePendingOutgoingAction(outgoingAction);
					}
				}
			}
		}
	}
	
	/**
	 * Queue an OutgoingAction to be sent to a connected client with matching clientId.
	 * @param clientId
	 * @param outgoingAction
	 */
	public void queueOutgoingAction(String clientId, OutgoingAction outgoingAction) {
		synchronized(clients) {
			if(clients.containsKey(clientId)){
				clients.get(clientId).queuePendingOutgoingAction(outgoingAction);
			}
		}
	}
	
	/**
	 * Gets all pending IncomingActions from all client objects.
	 * @return incomingActionList
	 */
	public LinkedList<IncomingAction> getPendingIncomingActions() {
		// Our list that will hold all IncomingActions from all connected clients.
		LinkedList<IncomingAction> incomingActionList = new LinkedList<IncomingAction>();
		synchronized(clients) {
			// Copy all pending actions.
			for(Client currentClient : clients.values()){
				// Don't bother collecting IncomingActions from disconnected clients.
				if(currentClient.isConnected()) {
					incomingActionList.addAll(currentClient.getPendingIncomingActions());
				}
			}
		}
		return incomingActionList;
	}
}
