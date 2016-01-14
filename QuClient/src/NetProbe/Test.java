package NetProbe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import Config.ClientConnectionConfig;
import FileTransfer.FileFormat;
import ProxyPlaylist.PlayList;
import Server.Device;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class Test {
	
	public static void main(String[] args) {
		findDevices();
		connectAndDisconnect();   // This can only be run locally.
		uploadPauseAndPlay();     // This can only be run locally.
	}

	/**
	 * Attempts to locate listening servers on the network and prints results.
	 */
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
		// Check that we actually got a ReachableQuDevice
		if(targetDevice != null) {
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
	
	/**
	 * Upload a track, pause it, and resume it.
	 */
	private static void uploadPauseAndPlay() {
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
		// Check that we actually got a ReachableQuDevice
		if(targetDevice != null) {
			// Create a Client Config
			ClientConnectionConfig config = new ClientConnectionConfig();
			config.setClientId(UUID.randomUUID().toString()); // Generate a random id so we can connect multiple clients
			config.setClientName("Nik");
			
			// Attempt to initialise our Device object
			Device runningQuServerDevice = null;
			try {
				runningQuServerDevice = new Device(targetDevice, config);
				
				// No problems so far, cross fingers and attempt to send a song!
				File audioFile = new File("TestAudiofiles/balls.mp3");
				runningQuServerDevice.uploadAudioFile(audioFile, FileFormat.MP3, "balls", "coolguy", "myalbum");
				
				// Wait for a bit
				try {
					Thread.sleep(6000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				 
				// Get latest playlist
				PlayList playList = runningQuServerDevice.getPlayList();
				
				if(playList.getTracks().size() == 0) {
					System.out.println("Empty PlayList!!!!");
				} else {
					// Pause the current track
					System.out.println("Pausing!!!!");
					playList.getTracks().get(0).pause();
				}
				
				try {
					Thread.sleep(6000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				// Get latest playlist
				playList = runningQuServerDevice.getPlayList();
				
				if(playList.getTracks().size() == 0) {
					System.out.println("Empty PlayList!!!!");
				} else {
					// Pause the current track
					System.out.println("Playing!!!!");
					playList.getTracks().get(0).play();;
				}
				
				// Wait for a bit
				try {
					Thread.sleep(6000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				 
				// Get latest playlist
				playList = runningQuServerDevice.getPlayList();
				
				if(playList.getTracks().size() == 0) {
					System.out.println("Empty PlayList!!!!");
				} else {
					// Pause the current track
					System.out.println("Pausing!!!!");
					playList.getTracks().get(0).pause();
				}
				
				try {
					Thread.sleep(6000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				// Get latest playlist
				playList = runningQuServerDevice.getPlayList();
				
				if(playList.getTracks().size() == 0) {
					System.out.println("Empty PlayList!!!!");
				} else {
					// Pause the current track
					System.out.println("Playing!!!!");
					playList.getTracks().get(0).play();;
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
