package FileTransfer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.UUID;
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
				String rawAudiofileJSON = audioFileInfoReader.readLine();
				JSONObject audioFileInfoJSON = new JSONObject(rawAudiofileJSON);
				
				// Initialise a new AudioFile object and populate it using the received JSON.
				AudioFile audioFile = new AudioFile();
				audioFile.setOwnerId(audioFileInfoJSON.getString("client_id"));
				audioFile.setName(audioFileInfoJSON.getString("name"));
				audioFile.setArtist(audioFileInfoJSON.getString("artist"));
				audioFile.setAlbum(audioFileInfoJSON.getString("album"));
				audioFile.setFileFormat(FileFormat.valueOf(audioFileInfoJSON.getString("format")));
				audioFile.setId(UUID.randomUUID().toString());
				audioFile.setPath(Server.properties.getUploadDirectory() + "/" + audioFile.getId());
				
				// Create physical file in the uploads directory
				File physicalAudioFile = new File(audioFile.getPath());
				// Attempt to create the file on the FileSystem.
				if(!physicalAudioFile.createNewFile()) {
					// We failed in creating the file, throw an exception.
					throw new RuntimeException("Could not create file");
				}
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
						while ((count = inputStream.read(bytes)) > 0) {
							fos.write(bytes, 0, count);
						}
						uploadComplete = true;
					} catch (IOException e) {
						// The possibility of getting an IOException here is very high.
						// Users can potentially cancel an upload or disconnect from the network/device. 
						// TODO LOG
					} finally {
						fos.close();
					}
				} catch (IOException e) {
					// TODO LOG
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
				// TODO LOG
			} catch (RuntimeException e) {
				// Most likely caused by not being able to create the file on disk.
				// TODO LOG
			}
		}
	}
}

// EXAMPLE AUDIOFILE
//AudioFile audioFile = new AudioFile(); 
//audioFile.setName("Great Song");
//audioFile.setId("g8d94jhd8");
//audioFile.setPath("temp/clean.mp3");
//audioFile.setAlbum("Rock Album");
//audioFile.setArtist("Mega-Band");
//audioFile.setOwnerId("NIKphoneid-dsfs2dfd34s");
//audioFile.setFileFormat(FileFormat.MP3);
