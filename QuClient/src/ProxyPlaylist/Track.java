package ProxyPlaylist;

import org.json.JSONObject;
import Server.Device;
import Server.OutgoingAction;
import Server.OutgoingActionType;

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
	
	public void play() {
		// TODO Check that we have permission to do this! 
		// (is this track ours? Is This Track in PAUSED state? are we super user?)
		if(trackState == TrackState.PAUSED) {
			// Create a new PLAY OutgoingAction
			JSONObject jsoPlay = new JSONObject();
			jsoPlay.put("track_id", trackId);
			// Queue this OutgoingAction
			device.sendAction(new OutgoingAction(OutgoingActionType.PLAY, jsoPlay));
		}
	}
	
	public void pause() {
		// TODO Check that we have permission to do this! 
		// (is this track ours? Is This Track in PLAYING state? are we super user?)
		if(trackState == TrackState.PLAYING) {
			// Create a new PAUSE OutgoingAction
			JSONObject jsoPause = new JSONObject();
			jsoPause.put("track_id", trackId);
			// Queue this OutgoingAction
			device.sendAction(new OutgoingAction(OutgoingActionType.PAUSE, jsoPause));
		}
	}
	
	public void stop() {
		// TODO do it!
	}
	
	public void move() {
		// TODO do it!
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
