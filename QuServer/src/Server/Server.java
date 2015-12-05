package Server;

import java.util.UUID;

import FileTransfer.AudioFile;
import FileTransfer.AudioFileReceiver;
import Media.Playlist;
import Properties.Properties;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class Server {
	private Playlist playlist = null;
	private AudioFileReceiver audioFileReceiver = null;
	public static Properties properties;
	private boolean isRunning = true;
	
	/**
	 * Entry point.
	 * @param args
	 */
	public static void main(String[] args) {
		Server server = new Server();
		server.start();
	}
	
	/**
	 * Carries out initialisations and tidy up before we hit the main server loop
	 */
	public void start() {
		// Load properties
		properties = new Properties();
		
		// Initialise the Log.
		Log.initialise();
		
		Log.log(Log.MessageType.INFO, "SERVER", "starting ...");
		
		// Check that the device has a unique ID, if we don't have one then generate one.
		if(properties.getDeviceId().equals("")) {
			String deviceId = UUID.randomUUID().toString();
			properties.setDeviceId(deviceId);
			Log.log(Log.MessageType.INFO, "SERVER", "Generated Device Id " + deviceId);
		}
	
		// Initialise and start AudioFileReceiver
		Log.log(Log.MessageType.INFO, "AUDIO_FILE_RECEIVER", "initialising...");
		audioFileReceiver = new AudioFileReceiver(properties.getAudioFileReceiverPort());
		Log.log(Log.MessageType.INFO, "AUDIO_FILE_RECEIVER", "initialised!");
		Log.log(Log.MessageType.INFO, "AUDIO_FILE_RECEIVER", "starting...");
		audioFileReceiver.start();
		Log.log(Log.MessageType.INFO, "AUDIO_FILE_RECEIVER", "started!");
		
		// Initialise PlayList
		Log.log(Log.MessageType.INFO, "PLAYLIST", "initialising...");
		playlist = new Playlist();
		Log.log(Log.MessageType.INFO, "PLAYLIST", "initialised!");
		
		// TODO Initialise ClientManager
		
		Log.log(Log.MessageType.INFO, "SERVER", "... started!");
		
		// We have finished setting up, start the system loop.
		systemLoop();
	}
	
	/**
	 * The main server loop
	 */
	private void systemLoop() {
		Log.log(Log.MessageType.INFO, "SERVER", "beginning system loop...");
		while(isRunning) {
			
			// TODO DO LOTS OF STUFF!!!!
			
			// Check to see if we have any completed file uploads, if so then add them to the playlist
			addPendingUploadsToPlaylist();
			
			// Allow the playlist to do some processing
			playlist.process();
			
			// If we have any changes to the system properties, write them to disk.
			if(properties.hasChanges()) {
				properties.write();
			}
			
			// If we are logging to disk we should write pending log entries.
			if(Log.writingToFile()) {
				Log.appendLogEntriesToFile();
			}
			
			// Sleep for a bit.
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void addPendingUploadsToPlaylist() {
		AudioFile pendingUpload = audioFileReceiver.getNextUpload();
		while(pendingUpload != null) {
			playlist.addTrack(pendingUpload);
			pendingUpload = audioFileReceiver.getNextUpload();
		}
	}
}
