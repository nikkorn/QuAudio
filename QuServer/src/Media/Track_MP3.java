package Media;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

import FileTransfer.AudioFile;
import Server.Log;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

public class Track_MP3 extends Playable {
	FileInputStream audioFileInputStream = null;
	Thread playThread = null;
	PlayRunnable playRunnable = null;
	int fileBytes = 0;
	boolean isPaused = false;
	
	public Track_MP3(AudioFile audioFile) {
		super(audioFile);
	}

	@Override
	public void initialise(boolean playOnReady) {
		Player player = null;
		
		try {
			audioFileInputStream = new FileInputStream(this.getAudioFile().getPath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			fileBytes = audioFileInputStream.available();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			player = new Player(audioFileInputStream);
		} catch (JavaLayerException e) {
			e.printStackTrace();
		}
		
		playRunnable = new PlayRunnable(player);
		playThread = new Thread(playRunnable);
		
		if(playOnReady) {
			play();
		}
	}
	
	@Override
	public void play() {
		Log.log(Log.MessageType.INFO, "TRACK", "play '" + this.getAudioFile().getName() + "'");
		// If our player thread is new then it needs to be started.
		if(playThread.getState() == Thread.State.NEW) {
			playThread.start();
		}
		playRunnable.setState(TrackState.PLAYING);
	}

	@Override
	public void pause() {
		Log.log(Log.MessageType.INFO, "TRACK", "pause '" + this.getAudioFile().getName() + "'");
		playRunnable.setState(TrackState.PAUSED);
	}

	@Override
	public void skipTo(double position) {
		Log.log(Log.MessageType.INFO, "TRACK", "skip to position '" + position + "' in '" + this.getAudioFile().getName() + "'");
		// TODO Finish
	}

	@Override
	public void stop() {
		Log.log(Log.MessageType.INFO, "TRACK", "stop '" + this.getAudioFile().getName() + "'");
		playRunnable.setState(TrackState.STOPPED);
	}

	@Override
	public void deleteAudioData() {
		Log.log(Log.MessageType.INFO, "TRACK", "delete audio data for '" + this.getAudioFile().getName() + "'");
		// TODO Delete the .mp3 off the disc
	}

	@Override
	public TrackState getState() {
		// This may need seeing to as the states are most likely not mapped up correctly
		return playRunnable.getState();
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
	
	public class PlayRunnable implements Runnable {
		private TrackState state = TrackState.PENDING;
		private Player player = null;
		
		PlayRunnable(Player player) {
			this.player = player;
		}
		
		public void run() {
			try {
				// Block while the file state is PENDING
				while(state == TrackState.PENDING) {}
				// Start the track loop, we should NOT do any intensive stuff here as that will mess up playback.
				do {
				    if(state == TrackState.PAUSED) {
				        LockSupport.park();
				    } else if(state == TrackState.STOPPED) {
				    	break;
				    }
				} while(player.play(1));
				// We have finished play all bytes in the track. Move to the STOPPED state.
				state = TrackState.STOPPED;
			} catch (JavaLayerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public TrackState getState() {
			return state;
		}

		public void setState(TrackState state) {
			this.state = state;
		}
	}
}