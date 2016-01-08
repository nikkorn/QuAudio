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
import ProxyPlaylist.PlayList;
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
	private Object settingsUpdateLock = new Object();
	private String deviceName;
	private boolean isProtected;
	private String[] superUsers;
	
	// Represents the latest state of our Playlist
	private JSONObject proxyPlaylist = null;
	// Reference to the last instance of PlayList that was constructed, this is needed as we will have to set 
	// the object as dirty the next time that the client receives a PUSH_PLAYLIST IncomingAction from the server
	private PlayList lastPlayList = null;
	
	public Device(ReachableQuDevice reachableDevice, ClientConnectionConfig clientConfig) throws IOException, RuntimeException {
		this.reachableDevice = reachableDevice;
		this.deviceName = reachableDevice.getDeviceName();
		this.isProtected = reachableDevice.isProtected();
		this.superUsers = reachableDevice.getSuperUserIds();
		// Lock the ClientConfig object so that its state cannot be altered from here on out.
		clientConfig.lock();
		this.clientConfig = clientConfig;
		// Create an ActionChannel.
		this.actionChannel = new ActionChannel(reachableDevice.getAddress(), reachableDevice.getClientManagerPort(), 
				clientConfig.getClientId(), clientConfig.getClientName(), clientConfig.getAccessPassword());
		
		// Start a new thread, this will be responsible for processing IncomingActions.
		Thread deviceThread = new Thread(new Runnable() {
			@Override
			public void run() {
				boolean connected = true;
				// Only process actions if we are still connected, otherwise don't bother.
				while(connected) {
					// Wait a bit so we don't hog too much processing power.
					try {
						Thread.sleep(6);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// Process incoming actions and check that we were still connected.
					connected = process();
				}
			}
		});
		deviceThread.setDaemon(true);
		deviceThread.start();
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
		synchronized(settingsUpdateLock) {
			return this.isProtected;
		}
	}

	public String getDeviceName() {
		synchronized(settingsUpdateLock) {
			return this.deviceName;
		}
	}
	
	public boolean adminModeEnabled() {
		synchronized(settingsUpdateLock) {
			for(String superClientId : superUsers) {
				if(superClientId.equals(clientConfig.getClientId())) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Returns the most up to date version of our PlayList since process() last handled a PUSH_PLAYLIST IncomingAction
	 * @return
	 */
	public PlayList getPlayList() {
		// Construct a new ordered list to store tracks
		LinkedList<Track> constructedPlayListTracks = new LinkedList<Track>();
		// Populate our list of tracks
		synchronized(proxyPlaylist) {
			JSONArray orderedTrackJSONArray = proxyPlaylist.getJSONArray("playlist");
			for(int trackIndex = 0; trackIndex < orderedTrackJSONArray.length(); trackIndex++) {
				JSONObject trackJSON = orderedTrackJSONArray.getJSONObject(trackIndex);
				Track newTrack = new Track();
				newTrack.setTrackId(trackJSON.getString("track_id"));
				newTrack.setOwnerId(trackJSON.getString("owner_id"));
				newTrack.setTrackState(TrackState.valueOf(trackJSON.getString("track_state")));
				newTrack.setName(trackJSON.getString("name"));
				newTrack.setArtist(trackJSON.getString("artist"));
				newTrack.setAlbum(trackJSON.getString("album"));
				// Add the new Track to the list
				constructedPlayListTracks.add(newTrack);
			}
		}
		PlayList constructedPlayList = new PlayList(constructedPlayListTracks);
		// Keep a reference to this object so we can mark it as dirty when we get an update on the PlayList
		this.lastPlayList = constructedPlayList;
		return constructedPlayList;
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
		// TODO check to see if this user has permissions for the action they want to send
		if(actionChannel.isConnected()) {
			actionChannel.sendOutgoingActionToServer(outgoingAction);
		} else {
			// We are no longer connected to the server
			throw new RuntimeException("not connected to Qu server");
		}
	}
	
	/**
	 * Process each IncomingAction
	 * @return Whether the connection was broken
	 */
	private boolean process() {
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
					// Store the JSON representation of our PlayList for the next time the user wants it.
					if(proxyPlaylist == null) {
						this.proxyPlaylist = incomingAction.getActionInfoObject();
					} else {
						synchronized(proxyPlaylist) {
							this.proxyPlaylist = incomingAction.getActionInfoObject();
						}
					}
					// We have a new snapshot of the state of the PlayList, if the user has grabbed a PlayList before 
					// we need to mark it as dirty to let them know it is out-dated.
					if(this.lastPlayList != null) {
						lastPlayList.setDirty(true);
					}
					break;
					
				case PUSH_SETTINGS:
					applySettingsUpdate(incomingAction);
					break;
				}
				
				// Fetch next incoming action
				incomingAction = actionChannel.getIncomingActionFromList();
			}
		} else {
			// We are disconnected, return that this failed
			return false;
		}
		// We are still connected, return true
		return true;
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
        synchronized(settingsUpdateLock) {
        	this.deviceName = settingsUpdateAction.getActionInfoObject().getString("device_name");
    		this.isProtected = settingsUpdateAction.getActionInfoObject().getBoolean("isProtected");
    		this.superUsers = superClientIds;
        }
	}
}
