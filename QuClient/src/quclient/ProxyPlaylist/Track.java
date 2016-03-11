package quclient.ProxyPlaylist;

import org.json.JSONException;
import org.json.JSONObject;
import quclient.Server.Device;
import quclient.Server.OutgoingAction;
import quclient.Server.OutgoingActionType;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class Track {
	private String trackId;
	private String ownerId;
	private String album;
	private String artist;
	private String name;
	private TrackState trackState;
	private Device device;
	
	public Track(Device parentDevice) {
		this.device = parentDevice;
	}
	
	/**
	 * Play this track as long as the current client has the right permissions and the track is currently paused.
	 */
	public void play() {
		// Check that we have permission to do this! (is this track ours? are we super user?)
		if(canInteract()) {
			// Is This Track in PAUSED state?
			if(trackState == TrackState.PAUSED) {
				// Create a new PLAY OutgoingAction
				JSONObject jsoPlay = new JSONObject();
				try {
					jsoPlay.put("track_id", trackId);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				// Queue this OutgoingAction
				device.sendAction(new OutgoingAction(OutgoingActionType.PLAY, jsoPlay));
			}
		} else {
			throw new RuntimeException("cannot manipulate this track, user is neither track owner nor super user");
		}
	}
	
	/**
	 * Pause this track as long as the current client has the right permissions and the track is currently playing.
	 */
	public void pause() {
		// Check that we have permission to do this! (is this track ours? are we super user?)
		if(canInteract()) {
			// Is This Track in PLAYING state?
			if(trackState == TrackState.PLAYING) {
				// Create a new PAUSE OutgoingAction
				JSONObject jsoPause = new JSONObject();
				try {
					jsoPause.put("track_id", trackId);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				// Queue this OutgoingAction
				device.sendAction(new OutgoingAction(OutgoingActionType.PAUSE, jsoPause));
			}
		} else {
			throw new RuntimeException("cannot manipulate this track, user is neither track owner nor super user");
		}
	}
	
	/**
	 * Stop this track as long as the current client has the right permissions.
	 */
	public void stop() {
		// Check that we have permission to do this! (is this track ours? are we super user?)
		if(canInteract()) {
			// Create a new STOP OutgoingAction
			JSONObject jsoStop = new JSONObject();
			try {
				jsoStop.put("track_id", trackId);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			// Queue this OutgoingAction
			device.sendAction(new OutgoingAction(OutgoingActionType.STOP, jsoStop));
		} else {
			throw new RuntimeException("cannot manipulate this track, user is neither track owner nor super user");
		}
	}
	
	/**
	 * Remove this track from the PlayList as long as the current client has the right permissions.
	 */
	public void remove() {
		// Check that we have permission to do this! (is this track ours? are we super user?)
		if(canInteract()) {
			// Create a new REMOVE OutgoingAction
			JSONObject jsoRemove = new JSONObject();
			try {
				jsoRemove.put("track_id", trackId);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			// Queue this OutgoingAction
			device.sendAction(new OutgoingAction(OutgoingActionType.REMOVE, jsoRemove));
		} else {
			throw new RuntimeException("cannot manipulate this track, user is neither track owner nor super user");
		}
	}
	
	/**
	 * Returns true if the current client has permission to interact (play,pause,stop .etc) with this track.
	 * @return Has permission
	 */
	public boolean canInteract() {
		return device.getClientConfiguration().getClientId().equals(this.ownerId) || device.adminModeEnabled();
	}
	
	public String getTrackId() {
		return trackId;
	}
	
	public void setTrackId(String trackId) {
		this.trackId = trackId;
	}
	
	public String getOwnerId() {
		return ownerId;
	}
	
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	
	public String getAlbum() {
		return album;
	}
	
	public void setAlbum(String album) {
		this.album = album;
	}
	
	public String getArtist() {
		return artist;
	}
	
	public void setArtist(String artist) {
		this.artist = artist;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public TrackState getTrackState() {
		return trackState;
	}

	public void setTrackState(TrackState trackState) {
		this.trackState = trackState;
	}
}
