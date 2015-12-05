package Media;

import java.util.ArrayList;

import FileTransfer.AudioFile;
import Server.Log;

public class Playlist {
	private ArrayList<Playable> tracks = new ArrayList<Playable>();
	
	/**
	 * Carries out processing on the playlist.
	 */
	public void process() {
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
			// TODO Other types of processing
		}
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
