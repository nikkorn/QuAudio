package quclient.ProxyPlaylist;

import java.util.LinkedList;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class PlayList {
	// Defines whether this instance of PlayList is out-dated (we have received a newer snapshot from the server)
	private volatile boolean isDirty = false;
	// Ordered list of tracks
	private LinkedList<Track> tracks;
	
	public PlayList(LinkedList<Track> tracks) {
		this.tracks = tracks;
	}

	/**
	 * Get an ordered list of the tracks that are in the PlayList
	 * @return
	 */
	public LinkedList<Track> getTracks() {
		return tracks;
	}

	/**
	 * Gets whether this instance of PlayList is out-dated and a newer version is available
	 * @return
	 */
	public boolean isDirty() {
		return isDirty;
	}

	/**
	 * Sets whether this instance of PlayList is out-dated and a newer version is available
	 * @param isDirty
	 */
	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
	}
}
