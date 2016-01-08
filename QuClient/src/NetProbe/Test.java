package NetProbe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import org.json.JSONObject;
import Config.ClientConnectionConfig;
import FileTransfer.FileFormat;
import ProxyPlaylist.PlayList;
import ProxyPlaylist.Track;
import Server.Device;
import Server.OutgoingAction;
import Server.OutgoingActionType;

public class Test {
	
	public static void main(String[] args) {
		//findDevices();
		//updateSettings();
		connectAndDisconnect();
	}

	public static void findDevices() {
		NetProbe probe = new NetProbe();
		if(probe.initialise()) {
			ArrayList<ReachableQuDevice> devices = probe.getReachableQuDevices(true);
			for(ReachableQuDevice device : devices) {
				System.out.println("DEVICE:");
				System.out.println("   ID        : " + device.getDeviceId());
				System.out.println("   NAME      : " + device.getDeviceName());
				System.out.println("   ADDRESS   : " + device.getAddress());
				System.out.println("   PROTECTED : " + device.isProtected());
			}
		} else {
			System.out.println("We Failed to initalise!!!");
		}
	}
	
	/**
	 * Send a test update_settings action to the server
	 */
	public static void updateSettings() {
		NetProbe probe = new NetProbe();
		ReachableQuDevice targetDevice = null;
		
		// Attempt to get local Qu server instance
		if(probe.initialise()) {
			ArrayList<ReachableQuDevice> devices = probe.getReachableQuDevices(true);
			for(ReachableQuDevice device : devices) {
				if(device.getAddress().equals("127.0.0.1")) {
					// We will be using the local instance on Qu server for this test
					targetDevice = device;
					break;
				}
			}
		} else {
			System.out.println("We Failed to initalise!!!");
		}
		
		if(targetDevice != null) {
			// We have a ReachableQuDevice object which represents the locally running Qu server.
			// Attempt to make a connection.
			
			// Create a Client Config
			ClientConnectionConfig config = new ClientConnectionConfig();
			config.setClientId(UUID.randomUUID().toString()); // Generate a random id so we can connect multiple clients
			config.setClientName("Nik");
			
			// Attempt to initialise our Device object
			Device runningQuServerDevice = null;
			try {
				runningQuServerDevice = new Device(targetDevice, config);
				
				// Write the action to the server
				// Create a dummy OutgoingAction which will hopefully be grabbed by the server as an IncomingAction for settings update
				//JSONObject testActionJSON = new JSONObject();
				//testActionJSON.put("device_name", "cake");
				//testActionJSON.put("access_password", "");
				//OutgoingAction testAction = new OutgoingAction(OutgoingActionType.UPDATE_SETTINGS, testActionJSON);
				// runningQuServerDevice.sendAction(testAction);
				
				// No problems so far, cross fingers and attempt to send a song!
				 File audioFile = new File("TestAudiofiles/balls.mp3");
				 runningQuServerDevice.uploadAudioFile(audioFile, FileFormat.MP3, "balls", "coolguy", "myalbum");
				
				// Dont let this test die
				while(true) {
					// Wait for a bit
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					// Get our playlist
					PlayList playList = runningQuServerDevice.getPlayList();
					// Print state of playlist
					System.out.println("------------------PlayList------------------");
					if(playList.getTracks().size() == 0) {
						System.out.println("Empty PlayList!");
					} else {
						for(Track track : playList.getTracks()) {
							System.out.println("ID: " + track.getTrackId());
							System.out.println("Name: " + track.getName());
							System.out.println("Artist: " + track.getArtist());
							System.out.println("Album: " + track.getAlbum());
							System.out.println("OwnerID: " + track.getOwnerId());
							System.out.println("State: " + track.getTrackState().toString());
							System.out.println();
						}
					}
					System.out.println("--------------------------------------------");
					
				}
				
			} catch (IOException e) {
				System.out.println("got IOException on Device constructor");
			} catch (RuntimeException e1) {
				System.out.println("got RuntimeException on Device constructor");
			} 
		} else {
			System.out.println("targetDevice is null!!!");
		}
	}
	
	/**
	 * Test connecting and disconnection to/from server
	 */
	private static void connectAndDisconnect() {
		NetProbe probe = new NetProbe();
		ReachableQuDevice targetDevice = null;
		
		// Attempt to get local Qu server instance
		if(probe.initialise()) {
			ArrayList<ReachableQuDevice> devices = probe.getReachableQuDevices(true);
			for(ReachableQuDevice device : devices) {
				if(device.getAddress().equals("127.0.0.1")) {
					// We will be using the local instance on Qu server for this test
					targetDevice = device;
					break;
				}
			}
		} else {
			System.out.println("We Failed to initalise!!!");
		}
		
		if(targetDevice != null) {
			// We have a ReachableQuDevice object which represents the locally running Qu server. Attempt to make a connection.
			// Create a Client Config
			ClientConnectionConfig config = new ClientConnectionConfig();
			config.setClientId(UUID.randomUUID().toString()); // Generate a random id so we can connect multiple clients
			config.setClientName("Nik");
			
			// Attempt to initialise our Device object
			Device runningQuServerDevice = null;
			try {
				runningQuServerDevice = new Device(targetDevice, config);
				
				// Sleep a bit before disconnecting
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				
				if(runningQuServerDevice.isConnected()) {
					runningQuServerDevice.disconnect();
				}
			
			} catch (IOException e) {
				System.out.println("got IOException on Device constructor");
			} catch (RuntimeException e1) {
				System.out.println("got RuntimeException on Device constructor");
			} 
		} else {
			System.out.println("targetDevice is null!!!");
		}
	}
}
