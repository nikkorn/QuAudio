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
	
	// Variables that are initially set by the input ReachableQuDevice, but can change during the lifetime of a Device object.
	private String deviceName;
	private boolean isProtected;
	private String[] superUsers;
	
	public Device(ReachableQuDevice reachableDevice, ClientConnectionConfig clientConfig) throws IOException, RuntimeException {
		this.reachableDevice = reachableDevice;
		this.deviceName = reachableDevice.getDeviceName();
		this.isProtected = reachableDevice.isProtected();
		this.superUsers = reachableDevice.getSuperUserIds();
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

	public int getAudioFileReceiverPort() {
		return reachableDevice.getAudioFileReceiverPort();
	}

	public int getClientManagerPort() {
		return reachableDevice.getClientManagerPort();
	}
	
	public boolean isProtected() {
		return this.isProtected;
	}

	public String getDeviceName() {
		return this.deviceName;
	}
	
	public boolean adminModeEnabled() {
		for(String superClientId : superUsers) {
			if(superClientId.equals(clientConfig.getClientId())) {
				return true;
			}
		}
		return false;
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
			// Catch any settings updates and apply them before handing the IncomingAction to the user.
			IncomingAction incomingAction = actionChannel.getIncomingActionFromList();
			if(incomingAction != null && incomingAction.getIncomingActionType() == IncomingActionType.PUSH_SETTINGS) {
				applySettingsUpdate(incomingAction);
			}
			return incomingAction;
		} else {
			// We are no longer connected to the server
			throw new RuntimeException("not connected to Qu server");
		}
	}
	
	/**
	 * Applies settings changes that were passed from the server
	 * @param settingsUpdateAction
	 */
	private void applySettingsUpdate(IncomingAction settingsUpdateAction) {
		// Construct a String array of super client ids
        String[] superClientIds = new String[settingsUpdateAction.getActionInfoObject().getJSONArray("super_users").length()];
        for(int i = 0; i < superClientIds.length; i++) {
        	superClientIds[i] = settingsUpdateAction.getActionInfoObject().getJSONArray("super_users").getString(i);
        }
		this.deviceName = settingsUpdateAction.getActionInfoObject().getString("device_name");
		this.isProtected = settingsUpdateAction.getActionInfoObject().getBoolean("isProtected");
		this.superUsers = superClientIds;
	}
}
