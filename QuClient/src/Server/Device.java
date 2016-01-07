package Server;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import org.json.JSONArray;
import org.json.JSONObject;
import Config.ClientConnectionConfig;
import FileTransfer.AudioFileSender;
import FileTransfer.FileFormat;
import NetProbe.ReachableQuDevice;
import ProxyPlaylist.Track;
import ProxyPlaylist.TrackState;

/**
 * Represents an instance of a Qu Server
 * @author Nikolas Howard
 *
 */
public class Device {
	private ReachableQuDevice reachableDevice;
	private ClientConnectionConfig clientConfig;
	private ActionChannel actionChannel;
	
	// Variables that are initially set by the input ReachableQuDevice, but can change during the lifetime of a Device object.
	private String deviceName;
	private boolean isProtected;
	private String[] superUsers;
	
	// Represents our Playlist
	private LinkedList<Track> proxyPlaylist;
	
	public Device(ReachableQuDevice reachableDevice, ClientConnectionConfig clientConfig) throws IOException, RuntimeException {
		this.reachableDevice = reachableDevice;
		this.deviceName = reachableDevice.getDeviceName();
		this.isProtected = reachableDevice.isProtected();
		this.superUsers = reachableDevice.getSuperUserIds();
		// Initialise our proxy playlist
		proxyPlaylist = new LinkedList<Track>();
		// Lock the ClientConfig object so that its state cannot be altered from here on out.
		clientConfig.lock();
		this.clientConfig = clientConfig;
		// Create an ActionChannel.
		this.actionChannel = new ActionChannel(reachableDevice.getAddress(), reachableDevice.getClientManagerPort(), 
				clientConfig.getClientId(), clientConfig.getClientName(), clientConfig.getAccessPassword());
	}
	
	public String getDeviceId() {
		return reachableDevice.getDeviceId();
	}

	public String getAddress() {
		return reachableDevice.getAddress();
	}

	public int getAudioFileReceiverPort() {
		return reachableDevice.getAudioFileReceiverPort();
	}

	public int getClientManagerPort() {
		return reachableDevice.getClientManagerPort();
	}
	
	public boolean isProtected() {
		return this.isProtected;
	}

	public String getDeviceName() {
		return this.deviceName;
	}
	
	public boolean adminModeEnabled() {
		for(String superClientId : superUsers) {
			if(superClientId.equals(clientConfig.getClientId())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the most up to date version of our PlayList since process() last handled a PUSH_PLAYLIST IncomingAction
	 * @return
	 */
	public LinkedList<Track> getPlaylist() {
		return proxyPlaylist;
	}
	
	/**
	 * Uploads an audio file to the Qu Server.
	 * @param audioFile
	 * @param format
	 * @param name
	 * @param artist
	 * @param album
	 */
	public void uploadAudioFile(File audioFile, FileFormat format, String name, String artist, String album) {
		AudioFileSender audioFileSender = new AudioFileSender(reachableDevice.getAddress(), reachableDevice.getAudioFileReceiverPort());
		try {
			audioFileSender.upload(audioFile, clientConfig.getClientId(), format.toString(), name, artist, album);
		} catch (UnknownHostException e) {
			// Upload failed, do nothing as this shouldn't effect the user,
			e.printStackTrace();
		} catch (IOException e) {
			// Upload failed, do nothing as this shouldn't effect the user,
			e.printStackTrace();
		}
	}
	
	/**
	 * Send an outgoing action to the server
	 * @param outgoingAction
	 */
	public void sendAction(OutgoingAction outgoingAction) {
		if(actionChannel.isConnected()) {
			actionChannel.sendOutgoingActionToServer(outgoingAction);
		} else {
			// We are no longer connected to the server
			throw new RuntimeException("not connected to Qu server");
		}
	}
	
	/**
	 * Process each IncomingAction
	 * @return
	 */
	public void process() {
		if(actionChannel.isConnected()) {
			// Catch any settings updates and apply them before handing the IncomingAction to the user.
			IncomingAction incomingAction = actionChannel.getIncomingActionFromList();
			// Process all actions
			while(incomingAction != null) {
				// Process each differently based on their type
				switch(incomingAction.getIncomingActionType()) {
				case PLAY_FAIL:
					break;
				case PUSH_PLAYLIST:
					applyPlaylistUpdate(incomingAction);
					break;
				case PUSH_SETTINGS:
					applySettingsUpdate(incomingAction);
					break;
				}
				
				// Fetch next incoming action
				incomingAction = actionChannel.getIncomingActionFromList();
			}
		} else {
			// We are no longer connected to the server
			throw new RuntimeException("not connected to Qu server");
		}
	}

	/**
	 * Returns true if there are pending incoming actions
	 * @return
	 */
	public boolean hasPendingActions(){
		return actionChannel.hasPendingIncomingActions();
	}
	
	/**
	 * Applies settings changes that were passed from the server
	 * @param settingsUpdateAction
	 */
	private void applySettingsUpdate(IncomingAction settingsUpdateAction) {
		// Construct a String array of super client ids
        String[] superClientIds = new String[settingsUpdateAction.getActionInfoObject().getJSONArray("super_users").length()];
        for(int i = 0; i < superClientIds.length; i++) {
        	superClientIds[i] = settingsUpdateAction.getActionInfoObject().getJSONArray("super_users").getString(i);
        }
		this.deviceName = settingsUpdateAction.getActionInfoObject().getString("device_name");
		this.isProtected = settingsUpdateAction.getActionInfoObject().getBoolean("isProtected");
		this.superUsers = superClientIds;
	}
	
	/**
	 * Applies playlist changes that were passed from the server
	 * @param settingsUpdateAction
	 */
	private void applyPlaylistUpdate(IncomingAction incomingAction) {
		// Get JSON array of tracks from the IncomingObject
		JSONArray orderedTrackJSONArray = incomingAction.getActionInfoObject().getJSONArray("playlist");
		// Empty the existing proxyPlaylist as it is out-dated and we will be replacing it with a brand new one.
		proxyPlaylist.clear();
		// Populate proxyPlaylist
		for(int trackIndex = 0; trackIndex < orderedTrackJSONArray.length(); trackIndex++) {
			JSONObject trackJSON = orderedTrackJSONArray.getJSONObject(trackIndex);
			Track newTrack = new Track();
			newTrack.setTrackId(trackJSON.getString("track_id"));
			newTrack.setOwnerId(trackJSON.getString("owner_id"));
			newTrack.setTrackState(TrackState.valueOf(trackJSON.getString("track_state")));
			newTrack.setName(trackJSON.getString("name"));
			newTrack.setArtist(trackJSON.getString("artist"));
			newTrack.setAlbum(trackJSON.getString("album"));
			// Add the new Track to the proxy playlist
			proxyPlaylist.add(newTrack);
		}
	}
}
