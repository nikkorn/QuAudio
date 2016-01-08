package Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import ClientManager.ClientManager;
import ClientManager.IncomingAction;
import ClientManager.OutgoingAction;
import ClientManager.OutgoingActionType;
import FileTransfer.AudioFile;
import FileTransfer.AudioFileReceiver;
import Media.Playlist;
import NetBeacon.Beacon;
import Properties.Properties;
import QuInterface.LEDBehaviour;
import QuInterface.LEDColourDefault;
import QuInterface.QuInterface;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class Server {
	private Playlist playlist = null;
	private AudioFileReceiver audioFileReceiver = null;
	private ClientManager clientManager = null;
	public static Properties properties;
	private boolean isRunning = true;
	
	/**
	 * Entry point.
	 * @param args
	 */
	public static void main(String[] args) {
		Server server = new Server();
		server.start();
	}
	
	/**
	 * Carries out initialisations and tidy up before we hit the main server loop
	 */
	public void start() {
		// Let us log how long it takes for server to start
		long startTime = System.currentTimeMillis();
		
		// Load properties
		properties = new Properties();
		
		// Initialise the Log.
		Log.initialise();
		
		Log.log(Log.MessageType.INFO, "SERVER", "starting...");
		
		// Check that the device has a unique ID, if we don't have one then generate one.
		if(properties.getDeviceId().equals("")) {
			String deviceId = UUID.randomUUID().toString();
			properties.setDeviceId(deviceId);
			Log.log(Log.MessageType.INFO, "SERVER", "Generated Device Id " + deviceId);
		}
		
		// Initialise and start NetProbe Beacon
		Log.log(Log.MessageType.INFO, "NETPROBE_BEACON", "initialising...");
		Beacon netprobeBeacon = new Beacon(properties.getNetProbeBeaconPort());
		Log.log(Log.MessageType.INFO, "NETPROBE_BEACON", "initialised!");
		Log.log(Log.MessageType.INFO, "NETPROBE_BEACON", "starting...");
		Thread beaconThread = new Thread(netprobeBeacon);
		beaconThread.setDaemon(true);
		beaconThread.start();
		Log.log(Log.MessageType.INFO, "NETPROBE_BEACON", "started!");
		
		// Initialise and start AudioFileReceiver
		Log.log(Log.MessageType.INFO, "AUDIO_FILE_RECEIVER", "initialising...");
		audioFileReceiver = new AudioFileReceiver(properties.getAudioFileReceiverPort());
		Log.log(Log.MessageType.INFO, "AUDIO_FILE_RECEIVER", "initialised!");
		Log.log(Log.MessageType.INFO, "AUDIO_FILE_RECEIVER", "starting...");
		audioFileReceiver.start();
		Log.log(Log.MessageType.INFO, "AUDIO_FILE_RECEIVER", "started!");
		
		// Initialise PlayList
		Log.log(Log.MessageType.INFO, "PLAYLIST", "initialising...");
		playlist = new Playlist();
		Log.log(Log.MessageType.INFO, "PLAYLIST", "initialised!");
		
		// Initialise ClientManager
		Log.log(Log.MessageType.INFO, "CLIENT_MANAGER", "initialising...");
		clientManager = new ClientManager(properties.getClientManagerPort());
		Log.log(Log.MessageType.INFO, "CLIENT_MANAGER", "initialised!");
		Log.log(Log.MessageType.INFO, "CLIENT_MANAGER", "starting...");
		clientManager.start();
		Log.log(Log.MessageType.INFO, "CLIENT_MANAGER", "started!");

		Log.log(Log.MessageType.INFO, "SERVER", "...started in " + (System.currentTimeMillis() - startTime) + "ms!");
		
		// We have finished setting up, start the system loop.
		systemLoop();
	}
	
	/**
	 * The main server loop
	 */
	private void systemLoop() {
		Log.log(Log.MessageType.INFO, "SERVER", "beginning system loop...");
		while(isRunning) {
			// Fetch and Process all pending IncomingActions from the ClientManager
			processIncomingActions();
			// Check to see if we have any completed file uploads, if so then add them to the playlist
			addPendingUploadsToPlaylist();
			// Allow modules to do some processing
			clientManager.process(this);
			playlist.process(this);
			// If we have any changes to the system properties, write them to disk.
			if(properties.hasChanges()) {
				properties.write();
			}
			// If we are logging to disk we should write pending log entries.
			if(Log.writingToFile()) {
				Log.appendLogEntriesToFile();
			}
			// Sleep for a bit.
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Fetch and Process all pending IncomingActions from the ClientManager
	 */
	private void processIncomingActions() {
		// Process each IncomingAction individually
		for(IncomingAction action : clientManager.getPendingIncomingActions()) {
			// What type of action is it?
			switch(action.getIncomingActionType()) {
			// A client has requested that we pause the currently playing audio file
			case PAUSE:
				break;
			
			// A client has requested that we play the current audio file (if it is paused)
			case PLAY:
				break;
				
			// A client has requested that we promote/demote a track in the playlist queue
			case MOVE:
				break;
				
			// A client has requested that we move to a different position in the currently playing track
			case SKIP:
				break;
				
			// A client has requested that we stop the currently playing/paused track and move on to the next (if there is one)
			case STOP:
				break;
				
			// A client has requested to become a super user, and has supplied an attempt at the super password
			case ADMIN_REQUEST:
				break;
				
			// A super client has supplied updated system settings
			case UPDATE_SETTINGS:
				properties.setDeviceName(action.getActionInfoObject().getString("device_name"));
				properties.setAccessPassword(action.getActionInfoObject().getString("access_password"));
				Log.log(Log.MessageType.INFO, "SERVER", "settings updated");
				// Broadcast a PUSH_SETTINGS action to all clients notifying them of the settings changing
				JSONObject settingsJSON = new JSONObject();
				// ------------------------------------------------
				// Add settings that will need to be sent
				String deviceName = Server.properties.getDeviceName();
				ArrayList<String> superUserClientIds = Server.properties.getSuperUsers();
				boolean isProtected = !Server.properties.getAccessPassword().equals("");
				// Create an independent JSON object to hold our super user info.
				JSONArray superUserJSONArray = new JSONArray();
				for(String suClientId : superUserClientIds) {
					superUserJSONArray.put(suClientId);
				}
				// Now that we have all of the information we need to send back to the client we can begin to construct our JSON response.
				settingsJSON.put("device_name", deviceName);
				settingsJSON.put("isProtected", isProtected);
				settingsJSON.put("super_users", superUserJSONArray);
				// ------------------------------------------------
				OutgoingAction settingsPushAction = new OutgoingAction(OutgoingActionType.PUSH_SETTINGS, settingsJSON);
				clientManager.queueOutgoingAction(settingsPushAction);
				break;
				
			// A super client has requested that the system shut down.
			case EXIT:
				Log.log(Log.MessageType.INFO, "SERVER", "system shutdown requested");
				// Interact with the QuInterface to set a standby (red) light
				QuInterface.show(LEDColourDefault.RED, LEDBehaviour.GLOW);
				// Shutdown the device (assumes this is unix based system)
				try {
					Runtime.getRuntime().exec("sudo shutdown -h now");
				} catch (IOException e) {
					Log.log(Log.MessageType.ERROR, "SERVER", "failed to shutdown");
				}
				break;
				
			// We have an unknown type, just ignore this IncomingAction
			default:
				Log.log(Log.MessageType.ERROR, "SERVER", "unknown action type '" + action.getIncomingActionType() + "'");
				break;
			}
		}
	}

	/**
	 * Adds any pending uploads to the PlayList
	 */
	private void addPendingUploadsToPlaylist() {
		AudioFile pendingUpload = audioFileReceiver.getNextUpload();
		while(pendingUpload != null) {
			playlist.addTrack(pendingUpload);
			pendingUpload = audioFileReceiver.getNextUpload();
		}
	}
	
	/**
	 * Returns the ClientManager
	 * @return clientManager
	 */
	public ClientManager getClientManager() {
		return clientManager;
	}
}
