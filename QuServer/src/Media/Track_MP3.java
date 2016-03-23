package Media;

import java.io.File;
import java.util.Map;
import FileTransfer.AudioFile;
import Server.Log;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

public class Track_MP3 extends Playable implements BasicPlayerListener {
	// Our player's BasicController.
	private BasicController control = null;
	// The state of this track.
	private volatile TrackState state = TrackState.PENDING;
	// Has the track been started before?
	private boolean trackIsNew = true;
	
	public Track_MP3(AudioFile audioFile) {
		super(audioFile);
	}

	@Override
	public void initialise(boolean playOnReady) {
		// Set up the player and controller.
		BasicPlayer player = new BasicPlayer();
		control = (BasicController) player;
		// Listen for player events.
		player.addBasicPlayerListener(this);
		
		try {			
			// Open the file.
			control.open(new File(this.getAudioFile().getPath()));
		} catch (BasicPlayerException e) {
			// We had an error setting up the player for this file, just set its state as STOPPED so the playlist dumps it.
			Log.log(Log.MessageType.ERROR, "TRACK", "error setting up player for '" + this.getAudioFile().getName() + "'");
			this.state = TrackState.STOPPED;
		}
		
		// Start playing the file if the playlist has requested it
		if(playOnReady) {
			play();
		}
	}
	
	@Override
	public void play() {
		Log.log(Log.MessageType.INFO, "TRACK", "play '" + this.getAudioFile().getName() + "'");
		// Play this track, whether its the first time or we are un-pausing.
		if(trackIsNew) {
			try {
				control.play();
			} catch (BasicPlayerException e) {
				// We had an error playing this file, just set its state as STOPPED so the playlist dumps it.
				Log.log(Log.MessageType.ERROR, "TRACK", "error playing track '" + this.getAudioFile().getName() + "'");
				this.state = TrackState.STOPPED;
			}
			// This track is no longer new
			trackIsNew = false;
			// Set the track state to playing.
			this.state = TrackState.PLAYING;
		} else {
			try {
				control.resume();
			} catch (BasicPlayerException e) {
				// We had an error resuming this file, just set its state as STOPPED so the playlist dumps it.
				Log.log(Log.MessageType.ERROR, "TRACK", "error resuming track '" + this.getAudioFile().getName() + "'");
				this.state = TrackState.STOPPED;
			}
			// Set the track state to playing.
			this.state = TrackState.PLAYING;
		}
	}

	@Override
	public void pause() {
		try {
			control.pause();
		} catch (BasicPlayerException e) {
			// We had an error pausing this file, just set its state as STOPPED so the playlist dumps it.
			Log.log(Log.MessageType.ERROR, "TRACK", "error pausing track '" + this.getAudioFile().getName() + "'");
			this.state = TrackState.STOPPED;
		}
		// Set the track state to playing.
		this.state = TrackState.PAUSED;
	}

	@Override
	public void skipTo(double position) {
		// TODO Finish
	}

	@Override
	public void stop() {
		try {
			control.pause();
		} catch (BasicPlayerException e) {
			// We had an error stopping this file.
			Log.log(Log.MessageType.ERROR, "TRACK", "error stopping track '" + this.getAudioFile().getName() + "'");
		}
		// Set the track state to playing.
		this.state = TrackState.STOPPED;
	}

	@Override
	public void deleteAudioData() {
		// TODO Delete the .mp3 off the disc
	}

	@Override
	public TrackState getState() {
		// This may need seeing to as the states are most likely not mapped up correctly
		return this.state;
	}

	@Override
	public double getDuration() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getPostion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void dispose() {
		
	}

	@Override
	public void stateUpdated(BasicPlayerEvent event) {
		// Listen for track stopped event.
		if(event.getCode() == BasicPlayerEvent.STOPPED) {
			this.state = TrackState.STOPPED;
		}
	}
	
	@Override
	public void opened(Object arg0, @SuppressWarnings("rawtypes") Map arg1) {}

	@Override
	public void progress(int arg0, long arg1, byte[] arg2, @SuppressWarnings("rawtypes") Map arg3) {}

	@Override
	public void setController(BasicController arg0) {}
}