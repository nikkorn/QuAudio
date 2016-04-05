package com.quaudio.quserver.media;

import com.quaudio.quserver.filetransfer.AudioFile;

/**
 * 
 * @author Nikolas Howard
 *
 */
public abstract class Playable {
	private AudioFile audioFile;
	
	public Playable(AudioFile audioFile) {
		this.audioFile = audioFile;
	}
	
	public AudioFile getAudioFile() {
		return audioFile;
	}
	
	public abstract void initialise(boolean playOnReady);
	public abstract void play();
	public abstract void pause();
	public abstract void skipTo(double position);
	public abstract void stop();
	public abstract void deleteAudioData();
	public abstract void dispose();
	public abstract TrackState getState();
	public abstract double getDuration();
	public abstract double getPostion();
}
