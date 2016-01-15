package quclient.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import org.json.JSONObject;

/**
 * Represents a channel of communication between the client and the server ClientManager
 * with which action request/responses are sent/received
 * @author Nikolas Howard
 *
 */
public class ActionChannel {
	// Is the ActionChannel connected to the QuServer ClientManager?
	private volatile boolean isConnected = false;
	// List of received IncomingActions to be processed
	private LinkedList<IncomingAction> pendingIncomingActions = new LinkedList<IncomingAction>();
	// A reference to the Listener which gets IncomingActions in a seperate thread
	private ActionChannelListener actionChannelListener;
	// Socket that defines connection between QuClient/QuServer
	private Socket actionChannelSocket;
	// PrintWriter used to send OutgoingAction JSON objects via our actionChannelSocket OutputStream
	private PrintWriter outgoingActionWriter;
	
	/**
	 * Constructor
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public ActionChannel(String serverAddress, int serverClientManagerPort, String clientId, String clientName, String accessPassword) throws UnknownHostException, IOException, RuntimeException {
		// Attempt to establish a connection to the server ClientManager
		actionChannelSocket = new Socket(serverAddress, serverClientManagerPort);
		// Initialise our PrintWriter
		outgoingActionWriter = new PrintWriter(actionChannelSocket.getOutputStream());
		// Initialise the BufferedReader thet will be used to get our handshake response, and
		// (if the connection is successful) will be passed to  new instance of ActionchannelListener
		BufferedReader socketBufferedReader = new BufferedReader(new InputStreamReader(actionChannelSocket.getInputStream()));
		// Send connection handshake and handle response
		// TODO may have to set a timeout on the socket for when we send the handshake as we may not get a response, and then remove it.
		switch(sendHandshake(clientId, clientName, accessPassword, socketBufferedReader)) {
		case ACCEPTED:
			// The handshake was a success, continue
			actionChannelListener = new ActionChannelListener(this, socketBufferedReader);
			// Set this Actionchannel as being connected to the server
			this.isConnected = true;
			break;
		case CONNECTION_FAILED:
			throw new IOException("error: connection failed");
		case DECLINED:
			throw new RuntimeException("server declined connection");
		case WRONG_ACCESS_PASSWORD:
			throw new RuntimeException("incorrect access password");
		case CLIENT_ALREADY_CONNECTED:
			throw new RuntimeException("client already connected");
		default:
			throw new RuntimeException("unknown response");
		}
	}

	/**
	 * Send Handshake to server ClientManager and wait for response to determine whenther connection failed/was accepted/was declined
	 * @param clientId
	 * @param clientName
	 * @param accessPassword
	 */
	private HandshakeResponse sendHandshake(String clientId, String clientName, String accessPassword, BufferedReader socketBufferedReader) {
		// Send a handshake as a JSON object to the server ClientManager
		JSONObject handshakeJSONObject = new JSONObject();
		handshakeJSONObject.put("client_id", clientId);
		handshakeJSONObject.put("client_name", clientName);
		handshakeJSONObject.put("access_password", accessPassword == null ? "" : accessPassword);
		// Send it
		outgoingActionWriter.println(handshakeJSONObject.toString());
		outgoingActionWriter.flush();
		// Get the servers response (will be a string in the form of "ACCEPTED","DECLINED" or "WRONG_ACCESS_PASSWORD")
		String rawHandshakeResponse = "";
		try {
			rawHandshakeResponse = socketBufferedReader.readLine();
		} catch (IOException e) {
			// We had a connection failure during the handshake, return the appropriate HandshakeResponse
			return HandshakeResponse.CONNECTION_FAILED;
		}
		HandshakeResponse response = null;
		// Cast the returned response as a HandshakeResponse enum
		try {
			response = HandshakeResponse.valueOf(rawHandshakeResponse.trim());
		} catch (IllegalArgumentException ile) {
			response = HandshakeResponse.UNIDENTIFIED;
		}
		// Return the HandshkeResponse value
		return response;
	}

	/**
	 * Takes an OutgoingAction and sends a JSON representation to the QuServer.
	 * @param outgoingAction
	 */
	public void sendOutgoingActionToServer(OutgoingAction outgoingAction) {
		outgoingActionWriter.println(outgoingAction.getActionInfoObject().toString());
		outgoingActionWriter.flush();
	}
	
	/**
	 * Called by the ActionChannelListener to add a received IncomingAction to our list of pending IncomingActions.
	 * @param newIncomingAction
	 */
	public void addIncomingAction(IncomingAction newIncomingAction) {
		synchronized(pendingIncomingActions) {
			pendingIncomingActions.add(newIncomingAction);
		}
	}
	
	/**
	 * Gets the oldest IncomingAction from pendingIncomingActions, returns null if empty
	 * @return oldest IncomingAction
	 */
	public IncomingAction getIncomingActionFromList() {
		IncomingAction incomingAction = null;
		synchronized(pendingIncomingActions) {
			if(pendingIncomingActions.size() > 0) {
				incomingAction = pendingIncomingActions.get(0);
				pendingIncomingActions.remove(0);
			}
		}
		return incomingAction;
	}
	
	
	/**
	 * Returns true if there are pending incoming actions
	 * @return
	 */
	public boolean hasPendingIncomingActions(){
		synchronized(pendingIncomingActions) {
			return pendingIncomingActions.size() > 0;
		}
	}
	
	/**
	 * Returns whether the ActionChannel and the ActionChannelListener are connected to 
	 * the server ClientManager (both must be connected)
	 * @return isConnected
	 */
	public boolean isConnected() {
		return this.isConnected && actionChannelListener.isConnected();
	}
	
	/**
	 * Disconnect from the server.
	 */
	public void disconnect() {
		// Closing our outgoingActionWriter will prompt the ClientActionListener on the
		// server to throw an IOException that will cause the ClientManager to regard
		// this client as disconnected, removing it from the client list.
		if(outgoingActionWriter != null) {
			outgoingActionWriter.close();
		}
		isConnected = false;
	}
	
	/**
	 * The potential outcomes of sending a handshake to the server
	 * @author Nikolas Howard
	 *
	 */
	public enum HandshakeResponse {
		ACCEPTED,
		WRONG_ACCESS_PASSWORD,
		CONNECTION_FAILED,
		DECLINED,
		CLIENT_ALREADY_CONNECTED,
		UNIDENTIFIED
	}
}
