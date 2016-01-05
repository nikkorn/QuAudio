package FileTransfer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.UUID;

import Server.Log;
import Server.Server;
import org.json.JSONObject;

import Media.FileFormat;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class DataTransfer implements Runnable{
	private Socket senderSocket = null;
	private AudioFileReceiver audioFileReceiver = null;
	
	public DataTransfer(Socket senderSocket, AudioFileReceiver receiver) {
		this.senderSocket = senderSocket;
		this.audioFileReceiver = receiver;
	}

	@Override
	public void run() {
		Log.log(Log.MessageType.INFO, "DATA_TRANSFER", "starting...");
		// Firstly, the client will be sending us a JSON object (as a string) containing the audio file information
		BufferedReader audioFileInfoReader = null;
		boolean gotFileInfo = true;
		try {
			audioFileInfoReader = new BufferedReader(new InputStreamReader(senderSocket.getInputStream()));
		} catch (IOException e) {
			// We failed in creating a reader, don't continue
			gotFileInfo = false;
			e.printStackTrace();
		}
		
		// Check we actually got the JSON without error.
		if(gotFileInfo) {
			try {
				// Read the JSON.
				Log.log(Log.MessageType.INFO, "DATA_TRANSFER", "reading audio file info JSON from input stream...");
				String rawAudiofileJSON = audioFileInfoReader.readLine();
				JSONObject audioFileInfoJSON = new JSONObject(rawAudiofileJSON);
				String uploadDirectory = Server.properties.getUploadDirectory();
				
				// Initialise a new AudioFile object and populate it using the received JSON.
				AudioFile audioFile = new AudioFile();
				audioFile.setOwnerId(audioFileInfoJSON.getString("client_id"));
				audioFile.setName(audioFileInfoJSON.getString("name"));
				audioFile.setArtist(audioFileInfoJSON.getString("artist"));
				audioFile.setAlbum(audioFileInfoJSON.getString("album"));
				audioFile.setFileFormat(FileFormat.valueOf(audioFileInfoJSON.getString("format")));
				audioFile.setId(UUID.randomUUID().toString());
				audioFile.setPath(uploadDirectory + "/" + audioFile.getId());
				
				Log.log(Log.MessageType.INFO, "DATA_TRANSFER", "created AudioFile object...");
				
				// Create physical file in the uploads directory
				File physicalAudioFile = new File(audioFile.getPath());
				// Attempt to create the file on the FileSystem.
				if(!physicalAudioFile.createNewFile()) {
					// We failed in creating the file, throw an exception.
					throw new RuntimeException("failed to create file '" + audioFile.getId() + "' in directory '" + uploadDirectory + "'");
				}
				
				Log.log(Log.MessageType.INFO, "DATA_TRANSFER", "created file '" + audioFile.getId() + "' in directory '" + uploadDirectory + "'");
				
				// Create FileOutputStream for writing to our file
				FileOutputStream fos = new FileOutputStream(physicalAudioFile);
				InputStream inputStream = null;
				byte[] bytes = new byte[1024];
				boolean uploadComplete = false;
				int count;
				
		        try {
					// Get the sockets InputStream
					inputStream = senderSocket.getInputStream();
					// Read bytes from stream and write them to disk.
				 	try {
				 		Log.log(Log.MessageType.INFO, "DATA_TRANSFER", "reading audio file from socket and writing to file...");
						while ((count = inputStream.read(bytes)) > 0) {
							fos.write(bytes, 0, count);
						}
						Log.log(Log.MessageType.INFO, "DATA_TRANSFER", "upload complete!");
						uploadComplete = true;
					} catch (IOException e) {
						// The possibility of getting an IOException here is very high.
						// Users can potentially cancel an upload or disconnect from the network/device. 
						Log.log(Log.MessageType.WARNING, "DATA_TRANSFER", "upload failed!");
					} finally {
						fos.close();
					}
				} catch (IOException e) {
					Log.log(Log.MessageType.WARNING, "DATA_TRANSFER", "failed to get socket inputstream");
				}
		        // If the upload has completed then we are done, if it wasn't for some reason then do nothing.
		        if(uploadComplete) {
		        	// --------------------------------------------------
					// TODO Extend by getting and comparing file checksum.
					// --------------------------------------------------
					// The upload is complete, add the AudioFile object to the list of pending files for processing.
					audioFileReceiver.addAudioFile(audioFile);
		        }
			} catch (IOException e) {
				// We had a networking issue while fetching our JSON.
				Log.log(Log.MessageType.ERROR, "DATA_TRANSFER", "failed to get audio file info JSON");
			} catch (RuntimeException e) {
				// Most likely an issue with creating the file on disk.
				Log.log(Log.MessageType.ERROR, "DATA_TRANSFER", e.getMessage());
			}
		}
	}
}
