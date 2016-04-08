package com.quaudio.quserver.filetransfer;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AudioFileReceiverUT {
	
	@Test 
	public void addAndGetPendingAudioFile() {
		// Create an instance of AudioFileReceiver.
		AudioFileReceiver afReceiver = new AudioFileReceiver(50100);
		// Attempt to fetch a pending audio file, none exist so we are expecting null.
		AudioFile nextAudioFile = afReceiver.getNextUpload();
		assertTrue("was expecting null value as there are no pending uploads", nextAudioFile == null);
		// Create an audio file and add it, simulating the completed upload of an audio file.
		AudioFile realAudioFile = new AudioFile();
		afReceiver.addAudioFile(realAudioFile);
		// Attempt to fetch another pending audio file, should get the audio file we created.
		nextAudioFile = afReceiver.getNextUpload();
		assertTrue("was expecting the audio file instance we added to the receiver", nextAudioFile == realAudioFile);
		// Now that we have fetched the only pending audio file, another call to 'getNextUpload()' should return null.
		nextAudioFile = afReceiver.getNextUpload();
		assertTrue("was expecting null value as there are no more pending uploads", nextAudioFile == null);
	}
	
	@Test 
	public void checkPendingAudioFileOrder() {
		// Create an instance of AudioFileReceiver.
		AudioFileReceiver afReceiver = new AudioFileReceiver(50110);
		// Create three audio files and add them.
		AudioFile firstAudioFile = new AudioFile();
		AudioFile secondAudioFile = new AudioFile();
		AudioFile thirdAudioFile = new AudioFile();
		afReceiver.addAudioFile(firstAudioFile);
		afReceiver.addAudioFile(secondAudioFile);
		afReceiver.addAudioFile(thirdAudioFile);
		// Get the three added audio files, they should be returned in the order they were added.
		assertTrue("was expecting the first audio file added", afReceiver.getNextUpload() == firstAudioFile);
		assertTrue("was expecting the second audio file added", afReceiver.getNextUpload() == secondAudioFile);
		assertTrue("was expecting the third and last audio file added", afReceiver.getNextUpload() == thirdAudioFile);
		assertTrue("was expecting null value as there are no more pending uploads", afReceiver.getNextUpload() == null);
	}
}
