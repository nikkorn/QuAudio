package com.quaudio.quclient.server;

import java.io.BufferedReader;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Listens for IncomingActions from the Qu server
 * @author Nikolas Howard
 *
 */
public class ActionChannelListener implements Runnable{
    // Is the ActionChannel connected to the QuServer ClientManager?
    private volatile boolean isConnected = true;
    // Reference to the ActionChannel
    private ActionChannel actionChannel;
    // Our BufferedReader with which we listen for IncomingActions from the server
    private BufferedReader socketBufferedReader;

    public ActionChannelListener(ActionChannel actionChannel, BufferedReader socketBufferedReader) {
        this.actionChannel = actionChannel;
        this.socketBufferedReader = socketBufferedReader;
        // Start the ActionChannelListener in a new thread as it will be blocking when waiting for input from the server.
        Thread actionChannelListenerThread = new Thread(this);
        actionChannelListenerThread.setDaemon(true);
        actionChannelListenerThread.start();
    }

    @Override
    public void run() {
        // The infinite loop in which we listen for IncomingActions from the server
        while(true) {
            String rawIncomingActionJSON = null;
            try {
                rawIncomingActionJSON = socketBufferedReader.readLine();
            } catch (IOException e) {
                // We have had a connection issue and therefore cannot continue
                // Set the ActionChannelListener as not connected and stop this thread
                isConnected = false;
                break;
            }
            JSONObject incomingActionJSONObject = null;
            // Check that our input isn't null, if it is then the connection could have been severed.
            if(rawIncomingActionJSON == null) {
                isConnected = false;
                break;
            }
            try {
                // Attempt to create a JSONObject using the read input
                incomingActionJSONObject = new JSONObject(rawIncomingActionJSON);
                // We were able to create a valid JSONObject, now make our IncomingAction
                IncomingAction newIncomingAction = new IncomingAction(incomingActionJSONObject);
                // Pass the new IncomingAction to the ActionChannel to be processed at a later time
                actionChannel.addIncomingAction(newIncomingAction);
            } catch (JSONException e) {
                // The raw JSON string that the server has sent us doesn't seem to be valid JSON.
                // Just do nothing and continue listening.
            }
        }
    }

    /**
     * Returns whether the ActionChannelListener is connected to the server ClientManager
     * @return isConnected
     */
    public boolean isConnected() {
        return isConnected;
    }
}
