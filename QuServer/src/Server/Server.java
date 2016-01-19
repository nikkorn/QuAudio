package Server;

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
			// Do we have any new connections? If so then they will need to be sent a welcome package ;)
			if(clientManager.hasNewClients()) {
				// We have had at least one newcomer.
				createAndQueueWelcomePackages();
			}
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
	 * Called when the ClientManager has received some new clients.
	 * Sends a welcome package of important OutgoingActions to these newbies.
	 */
	private void createAndQueueWelcomePackages() {
		ArrayList<OutgoingAction> welcomePackage = new ArrayList<OutgoingAction>();
		// Add PUSH_PLAYLIST to welcome package
		welcomePackage.add(playlist.generatePushPlayListOutgoingAction());
		// Add PUSH_VOLUME to welcome package
		JSONObject volumeSetJSON = new JSONObject();
		volumeSetJSON.put("volume_level", Utils.getMasterVolume());
		welcomePackage.add(new OutgoingAction(OutgoingActionType.PUSH_VOLUME, volumeSetJSON));
		clientManager.sendWelcomePackages(welcomePackage);
	}

	/**
	 * Fetch and Process all pending IncomingActions from the ClientManager
	 */
	private void processIncomingActions() {
		// Process each IncomingAction individually
		for(IncomingAction action : clientManager.getPendingIncomingActions()) {
			// What type of action is it?
			switch(action.getIncomingActionType()) {
			case PAUSE:	// A client has requested that we pause the currently playing audio file
			case PLAY:	// A client has requested that we play the current audio file (if it is paused)
			case MOVE:	// A client has requested that we promote/demote a track in the playlist queue
			case SKIP:	// A client has requested that we move to a different position in the currently playing track
			case STOP:	// A client has requested that we stop the currently playing/paused track and move on to the next (if there is one)
			case REMOVE:// A client has requested that we remove a track
				// It is the responsibility of the PlayList to process this IncomingAction.
				playlist.processAction(action);
				break;
				
			// A client has requested to become a super user, and has supplied an attempt at the super password
			case ADMIN_REQUEST:
				break;
			
			// A super user client has requested to change the system volume.
			case UPDATE_VOLUME:
				// Check that this user actually is a super user.
				if(properties.isSuperUser(action.getActionInfoObject().getString("client_id"))) {
					// Set the system volume.
					int newVolumeLevel = action.getActionInfoObject().getInt("volume_level");
					Utils.setMasterVolume(newVolumeLevel);
					// Notify all connected users about the change in volume.
					JSONObject volumeSetJSON = new JSONObject();
					volumeSetJSON.put("volume_level", newVolumeLevel);
					OutgoingAction volumeSetPushAction = new OutgoingAction(OutgoingActionType.PUSH_VOLUME, volumeSetJSON);
					clientManager.queueOutgoingAction(volumeSetPushAction);
				}
				break;
				
			// A super client has supplied updated system settings
			case UPDATE_SETTINGS:
				// Updating the server settings is serious business, double check that this user is super.
				if(!properties.isSuperUser(action.getActionInfoObject().getString("client_id"))) {
					Log.log(Log.MessageType.WARNING, "SERVER", "we received an UPDATE_SETTINGS IncomingAction from a non-super user! Ignoring it.");
					break;
				}
				properties.setDeviceName(action.getActionInfoObject().getString("device_name"));
				// Only set the access password if it is not '?' ('?' is sent by the client if they do not want to alter this value)
				if(!action.getActionInfoObject().getString("access_password").equals("?")) {
					properties.setAccessPassword(action.getActionInfoObject().getString("access_password"));
				}
				// Only set the super password if it is not '?' ('?' is sent by the client if they do not want to alter this value)
				if(!action.getActionInfoObject().getString("super_password").equals("?")) {
					properties.setSuperPassword(action.getActionInfoObject().getString("super_password"));
				}
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
				// Attempt to shutdown the device.
				Utils.shutdownDevice(Server.properties.isOSUnixLike());
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
