package ProxyPlaylist;

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
