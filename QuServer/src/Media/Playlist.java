package Media;

import java.util.LinkedList;
import org.json.JSONArray;
import org.json.JSONObject;
import ClientManager.IncomingAction;
import ClientManager.OutgoingAction;
import ClientManager.OutgoingActionType;
import FileTransfer.AudioFile;
import Server.Log;
import Server.Server;

public class Playlist {
	// The tracks that are currently in the playlist
	private LinkedList<Playable> tracks = new LinkedList<Playable>();
	// Has the PlayList been altered (are we required to send a new PUSH_PLAYLIST?)
	private boolean isPushPlayListPending = false;
	
	/**
	 * Carries out processing on the playlist.
	 * @param server 
	 */
	public void process(Server server) {
		// Only do processing when there are tracks to process.
		if(tracks.size() > 0){
			// Check that the current track is not stopped, if it is then skip it.
			if(this.getCurrentTrack().getState() == TrackState.STOPPED) {
				this.getCurrentTrack().dispose();
				tracks.remove(0);
				if(tracks.size() > 0) {
					this.getCurrentTrack().play();
					// We need to broadcast change
					setPushPlayListPending(true);
				}
			}
		}
		// Has the state of the PlayList changed since the last time we broadcast it's state to clients?
		// If so then we will need to send it again.
		if(isPushPlayListPending()) {
			// Queue a new PUSH_PLAYLIST OutgoingAction in the ClientManager to be broadcast to all connected clients
			server.getClientManager().queueOutgoingAction(this.generatePushPlayListOutgoingAction());
		}
	}
	
	/**
	 * Process an IncomingAction that manipulates this PlayList.
	 * @param action
	 */
	public void processAction(IncomingAction action) {
		// What type of PlayList IncomingAction is it?
		// First, get the track id from the IncomingAction.
		String incomingActionTrackId = action.getActionInfoObject().getString("track_id");
		// Process the action depending on its type.
		switch(action.getIncomingActionType()) {
		case PLAY:
			// The track that the user wants to play may no longer be at the 
			// top of the queue, or may have stopped, so only play it if the 
			// current track is in PAUSED state and that it has an id matching
			// the one supplied in the IncomingAction. 
			// Get the current track.	
			Playable currentTrackToPlay = this.getCurrentTrack();
			// Check that the current track is paused, and that the id's match.
			if(currentTrackToPlay != null && incomingActionTrackId.equals(currentTrackToPlay.getAudioFile().getId()) 
					&& (currentTrackToPlay.getState() == TrackState.PAUSED)) {
				// Conditions are right, play the currently paused track.
				currentTrackToPlay.play();
				// We need to broadcast change
				setPushPlayListPending(true);
			}
			
			break;
		case PAUSE:
			// The track that the user wants to pause may no longer be at the 
			// top of the queue, or may have stopped, so only pause it if the 
			// current track is in PLAYING state and that it has an id matching
			// the one supplied in the IncomingAction. 
			// Get the current track.	
			Playable currentTrackToPause = this.getCurrentTrack();
			// Check that the current track is playing, and that the id's match.
			if(currentTrackToPause != null && incomingActionTrackId.equals(currentTrackToPause.getAudioFile().getId()) 
					&& (currentTrackToPause.getState() == TrackState.PLAYING)) {
				// Conditions are right, pause the currently playing track.
				currentTrackToPause.pause();
				// We need to broadcast change
				setPushPlayListPending(true);
			}
			break;
		case STOP:
			// The track that the user wants to stop may no longer be at the 
			// top of the queue, or may have stopped already, so only stop it if the 
			// current track has an id matching to the one supplied in the IncomingAction. 
			// Get the current track.	
			Playable currentTrackToStop = this.getCurrentTrack();
			// Check that the id's match.
			if(currentTrackToStop != null && incomingActionTrackId.equals(currentTrackToStop.getAudioFile().getId())) {
				// Conditions are right, stop the currently playing track.
				currentTrackToStop.stop();
				// We need to broadcast change
				setPushPlayListPending(true);
			}
			break;
		case MOVE:
			// TODO Finish!
			// We need to broadcast change
			setPushPlayListPending(true);
			break;
		case SKIP:
			// TODO Finish!
			// We need to broadcast change
			setPushPlayListPending(true);
			break;
		default:
			// Unknown, do nothing.
			break;
		}
	}
	
	/**
	 * Creates and returns a new PUSH_PLAYLIST OutgoingAction.
	 * @return PUSH_PLAYLIST OutgoingAction
	 */
	public OutgoingAction generatePushPlayListOutgoingAction() {
		JSONObject playListJSONObject = new JSONObject();
		JSONArray playListJSONArray = new JSONArray();
		// Go over each track in our PlayList and write the details to our playListJSONArray JSON array
		for(Playable playable : tracks) {
			JSONObject trackJSON = new JSONObject();
			trackJSON.put("track_id", playable.getAudioFile().getId());
			trackJSON.put("owner_id", playable.getAudioFile().getOwnerId());
			trackJSON.put("track_state", playable.getState().toString());
			trackJSON.put("name", playable.getAudioFile().getName());
			trackJSON.put("artist", playable.getAudioFile().getArtist());
			trackJSON.put("album", playable.getAudioFile().getAlbum());
			// Add this track to out JSON array
			playListJSONArray.put(trackJSON);
		}
		// Put our PlayList JSON array in its own object.
		playListJSONObject.put("playlist", playListJSONArray);
		return new OutgoingAction(OutgoingActionType.PUSH_PLAYLIST, playListJSONObject);
	}

	/**
	 * Creates a Playable object and adds it to the PlayList.
	 * @param audioFile
	 */
	public void addTrack(AudioFile audioFile){
		// Initialise a new Playable object.
		Playable track = null;
		// The file format will define which subclass of Playable we will need.
		switch(audioFile.getFileFormat()) {
		case FLAC:
			track = new Track_FLAC(audioFile);
			break;
		case GP3:
			track = new Track_3GP(audioFile);
			break;
		case MP3:
			track = new Track_MP3(audioFile);
			break;
		case MP4:
			track = new Track_MP4(audioFile);
			break;
		case WAV:
			track = new Track_WAV(audioFile);
			break;
		default:
			// TODO Error, something is fishy here
			break;
		}
		// TODO Check if there is an existing track that matches the one that is currently playing, if so 
		// then don't play it and notify the user who attempted to push it.
		tracks.add(track);
		Log.log(Log.MessageType.INFO, "PLAYLIST", "added track '" + audioFile.getName() + "'");
		// If the track that we have just added is the only track in the playlist, then just start playing it.
		if(tracks.size() == 1) {
			track.initialise(true);
		} else {
			track.initialise(false);
		}
		// We need to broadcast change
		setPushPlayListPending(true);
	}
	
	/**
	 * Gets the currently active track, if the playlist is empty then this returns null.
	 */
	public Playable getCurrentTrack() {
		if(tracks.size() > 0) {
			return tracks.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Are we due a PUSH_PLAYLIST broadcast?
	 * @return
	 */
	public boolean isPushPlayListPending() {
		boolean pushPending = this.isPushPlayListPending;
		this.isPushPlayListPending = false;
		return pushPending;
	}

	/**
	 * Set whether we are due a PUSH_PLAYLIST broadcast?
	 * @param isPushPlayListPending
	 */
	public void setPushPlayListPending(boolean isPushPlayListPending) {
		this.isPushPlayListPending = isPushPlayListPending;
	}
}
