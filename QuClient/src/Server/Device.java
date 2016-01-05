package Server;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import Config.ClientConnectionConfig;
import FileTransfer.AudioFileSender;
import FileTransfer.FileFormat;
import NetProbe.ReachableQuDevice;

/**
 * Represents an instance of a Qu Server
 * @author Nikolas Howard
 *
 */
public class Device {
	private ReachableQuDevice reachableDevice;
	private ClientConnectionConfig clientConfig;
	private ActionChannel actionChannel;
	
	public Device(ReachableQuDevice reachableDevice, ClientConnectionConfig clientConfig) throws IOException, RuntimeException {
		this.reachableDevice = reachableDevice;
		// Lock the ClientConfig object so that its state cannot be altered from here on out.
		clientConfig.lock();
		this.clientConfig = clientConfig;
		// Create an ActionChannel.
		this.actionChannel = new ActionChannel(reachableDevice.getAddress(), reachableDevice.getClientManagerPort(), 
				clientConfig.getClientId(), clientConfig.getClientName(), clientConfig.getAccessPassword());
	}
	
	public String getDeviceId() {
		return reachableDevice.getDeviceId();
	}

	public String getAddress() {
		return reachableDevice.getAddress();
	}

	public boolean isProtected() {
		return reachableDevice.isProtected();
	}

	public String getDeviceName() {
		return reachableDevice.getDeviceName();
	}

	public int getAudioFileReceiverPort() {
		return reachableDevice.getAudioFileReceiverPort();
	}

	public int getClientManagerPort() {
		return reachableDevice.getClientManagerPort();
	}
	
	/**
	 * Uploads an audio file to the Qu Server.
	 * @param audioFile
	 * @param format
	 * @param name
	 * @param artist
	 * @param album
	 */
	public void uploadAudioFile(File audioFile, FileFormat format, String name, String artist, String album) {
		AudioFileSender audioFileSender = new AudioFileSender(reachableDevice.getAddress(), reachableDevice.getAudioFileReceiverPort());
		try {
			audioFileSender.upload(audioFile, clientConfig.getClientId(), format.toString(), name, artist, album);
		} catch (UnknownHostException e) {
			// Upload failed, do nothing as this shouldn't effect the user,
			e.printStackTrace();
		} catch (IOException e) {
			// Upload failed, do nothing as this shouldn't effect the user,
			e.printStackTrace();
		}
	}
	
	/**
	 * Send an outgoing action to the server
	 * @param outgoingAction
	 */
	public void sendAction(OutgoingAction outgoingAction) {
		if(actionChannel.isConnected()) {
			actionChannel.sendOutgoingActionToServer(outgoingAction);
		} else {
			// We are no longer connected to the server
			throw new RuntimeException("not connected to Qu server");
		}
	}
	
	/**
	 * Fetch the next pending IncomingAction from the server. Will return null if there are no pending actions.
	 * @return
	 */
	public IncomingAction fetchAction() {
		if(actionChannel.isConnected()) {
			return actionChannel.getIncomingActionFromList();
		} else {
			// We are no longer connected to the server
			throw new RuntimeException("not connected to Qu server");
		}
	}
}
