package ClientManager;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.LinkedList;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class Client {
	// Queue of pending outgoing actions that are to be sent to this client.
	private LinkedList<OutgoingAction> outgoingActionQueue = new LinkedList<OutgoingAction>();
	// Queue of pending incoming actions that are to be processed by the server.
	private LinkedList<IncomingAction> incomingActionQueue = new LinkedList<IncomingAction>();
	// Is the connection alive?
	private volatile boolean isConnected = true;
	// The PrintWriter responsible for sending action JSON to this client
	private PrintWriter outgoingActionPrintWriter;
	// The ClientActionListener that runs in its own thread listening for actions from this client
	private ClientActionListener clientActionListener;
	// Is this a new client? (will require a welcome package)
	private boolean isNewClient = true;
	
	public Client(BufferedReader actionReader, PrintWriter actionWriter) {
		// Set the outgoingActionPrintWriter with which we will be sending all OutgoingActions
		outgoingActionPrintWriter = actionWriter;
		// Initialise and start clientActionListener
		clientActionListener = new ClientActionListener(this, actionReader);
		Thread clientActionListenerThread = new Thread(clientActionListener);
		clientActionListenerThread.setDaemon(true);
		clientActionListenerThread.start();
	}
	
	/**
	 * Queues a pending OutgoingAction that is to be sent to the client. 
	 * @param outgoingAction
	 */
	public void queuePendingOutgoingAction(OutgoingAction outgoingAction) {
		synchronized(outgoingActionQueue) {
			outgoingActionQueue.add(outgoingAction);
		}
	}
	
	/**
	 * Called by the clientActionListener when we get a valid IncomingAction
	 * @param newIncomingAction
	 */
	public void queueIncomingAction(IncomingAction newIncomingAction) {
		synchronized(incomingActionQueue) {
			incomingActionQueue.add(newIncomingAction);
		}
	}
	
	/**
	 * Sends all pending OutgoingActions to the client and clears the list.
	 */
	public void sendPendingOutgoingActions() {
		synchronized(outgoingActionQueue) {
			// Send each pending OutgoingAction to the client.
			for(OutgoingAction currentAction : outgoingActionQueue){
				outgoingActionPrintWriter.println(currentAction.getActionInfoObject().toString());
				outgoingActionPrintWriter.flush();
			}
			// Clear the list of OutgoingActions
			outgoingActionQueue.clear();
		}
	}
	
	/**
	 * Gets all pending IncomingActions that was sent by the client and clears the list.
	 * @return incomingActionList
	 */
	public LinkedList<IncomingAction> getPendingIncomingActions() {
		LinkedList<IncomingAction> incomingActionList = new LinkedList<IncomingAction>();
		synchronized(incomingActionQueue) {
			// Copy all pending actions.
			for(IncomingAction currentAction : incomingActionQueue){
				incomingActionList.add(currentAction);
			}
			// Clear the list of IncomingActions
			incomingActionQueue.clear();
		}
		return incomingActionList;
	}
	
	/**
	 * Returns true/false depending on whether this client is connected.
	 * @return isConnected
	 */
	public boolean isConnected() {
		return isConnected && clientActionListener.isConnected();
	}

	/**
	 * Returns true if this is a new client (requires welcome package)
	 * @return isNewClient
	 */
	public boolean isNewClient() {
		boolean isNew = this.isNewClient;
		this.isNewClient = false;
		return isNew;
	}
}
