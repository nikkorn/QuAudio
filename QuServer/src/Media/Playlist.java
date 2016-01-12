package Media;

import java.util.LinkedList;
import org.json.JSONArray;
import org.json.JSONObject;
import ClientManager.ClientManager;
import ClientManager.IncomingAction;
import ClientManager.OutgoingAction;
import ClientManager.OutgoingActionType;
import FileTransfer.AudioFile;
import Server.Log;
import Server.Server;

public class Playlist {
	// The tracks that are currently in the playlist
	private LinkedList<Playable> tracks = new LinkedList<Playable>();
	// The last time that the PlayList queued a PUSH_PLAYLIST in the ClientManager
	private long lastPlayListActionPush;
	// The number of milliseconds between each time the PlayList will queue a PUSH_PLAYLIST action
	private long playListPushWait = 333;
	
	public Playlist() {
		lastPlayListActionPush = System.currentTimeMillis();
	}
	
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
				}
			}
		}
		
		// Check to see if we are due a push to clients
		long currentTimeMs = System.currentTimeMillis();
		if((currentTimeMs - lastPlayListActionPush) > playListPushWait) {
			// Queue a PUSH_PLAYLIST action
			queuePlaylistPushAction(server.getClientManager());
			lastPlayListActionPush = currentTimeMs;
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
			if(incomingActionTrackId.equals(currentTrackToPlay.getAudioFile().getId()) 
					&& (currentTrackToPlay.getState() == TrackState.PAUSED)) {
				// Conditions are right, play the currently paused track.
				currentTrackToPlay.play();
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
			if(incomingActionTrackId.equals(currentTrackToPause.getAudioFile().getId()) 
					&& (currentTrackToPause.getState() == TrackState.PLAYING)) {
				// Conditions are right, pause the currently playing track.
				currentTrackToPause.pause();
			}
			break;
		case STOP:
			// TODO Finish!
			break;
		case MOVE:
			// TODO Finish!
			break;
		case SKIP:
			// TODO Finish!
			break;
		default:
			// Unknown, do nothing.
			break;
		}
	}
	
	/**
	 * Queues a PUSH_PLAYLIST OutgoingAction with the ClientManager.
	 * @param clientManager 
	 * @param server 
	 */
	private void queuePlaylistPushAction(ClientManager clientManager) {
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
		// Queue a new PUSH_PLAYLIST OutgoingAction in the ClientManager to be broadcast to all connected clients
		clientManager.queueOutgoingAction(new OutgoingAction(OutgoingActionType.PUSH_PLAYLIST, playListJSONObject));
	}

	/**
	 * Creates a Playable object and adds it to the playlist.
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
	 * Immediately stops the current song, clears it off the stack, and initialises and plays the next.
	 */
	public void skip() {
		Log.log(Log.MessageType.INFO, "PLAYLIST", "skipping track '" + getCurrentTrack().getAudioFile().getName() + "'");
		
		// If the track we want to skip is still active (not stopped) then stop it before removing it.
		if(getCurrentTrack().getState() != TrackState.STOPPED) {
			getCurrentTrack().stop();
		}
		
		// TODO add an abstract cleanup method to Playable to be called here.
		
		// Delete audio data for this file.
		getCurrentTrack().deleteAudioData();
		
		// Remove the track  we've finished with
		tracks.remove(0);
		
		// Check to see if there is another song to skip to, if not then do nothing, otherwise initialise and play it.
		if(tracks.size() > 0) {
			getCurrentTrack().play();
		} 
	}
}
