package FileTransfer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import Server.Log;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class AudioFileReceiver implements Runnable {
	private ServerSocket audioFileReceiverServerSocket = null;
	private LinkedList<AudioFile> pendingUploadedAudioFiles = new LinkedList<AudioFile>();
	
	public AudioFileReceiver(int port) {	
		try {
			audioFileReceiverServerSocket = new ServerSocket(port);
		} catch (IOException e) {
			// If this fails they quit the app.
			Log.log(Log.MessageType.CRITICAL, "AUDIO_FILE_RECEIVER", "failed to initialise AudioFileReceiver server socket");
			Log.forceWrite();
			System.exit(0);
		}
	}
	
	public void start() {
		// The AudioFileReceiver needs to be started on its own thread.
		Thread audioFileReceiverThread = new Thread(this);
		audioFileReceiverThread.setDaemon(true);
		audioFileReceiverThread.start();
	}

	@Override
	public void run() {
		// Forever listen for and handle incoming uploads
		while(true) {
			boolean gotSocket = true;
			Socket senderSocket = null;
			try {
				senderSocket = audioFileReceiverServerSocket.accept();
			} catch (IOException e) {
				// If we get an IOException here then just log it as an unsuccesful connection.
				gotSocket = false;
				Log.log(Log.MessageType.WARNING, "AUDIO_FILE_RECEIVER", "failed to get connecting client socket");
			}
			// Only continue if we successfully grabbed a Socket.
			if(gotSocket) {
				Log.log(Log.MessageType.INFO, "AUDIO_FILE_RECEIVER", "established connection with client at '" 
						+ senderSocket.getRemoteSocketAddress().toString() + "'");
				// Handling of individual upload requests are intensive and must happed on independent thread.
				Thread dataTransferThread = new Thread(new DataTransfer(senderSocket, this));
				dataTransferThread.setDaemon(true);
				dataTransferThread.start();
			}
		}
	}
	
	public void addAudioFile(AudioFile audioFile) {
		Log.log(Log.MessageType.INFO, "AUDIO_FILE_RECEIVER", "adding '" + audioFile.getName() + "'");
		synchronized(pendingUploadedAudioFiles) {
			pendingUploadedAudioFiles.add(audioFile);
		}
	}
	
	public AudioFile getNextUpload() {
		AudioFile audioFile = null;
		synchronized(pendingUploadedAudioFiles) {
			// Only continue if we have at least one finished upload.
			if(pendingUploadedAudioFiles.size() > 0) {
				audioFile = pendingUploadedAudioFiles.iterator().next();
			}
			// Remove this AudioFile from the list.
			pendingUploadedAudioFiles.remove(audioFile);
		}
		return audioFile;
	}
}
