package NetProbe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import Config.ClientConnectionConfig;
import FileTransfer.FileFormat;
import Server.Device;

public class Test {
	
	public static void main(String[] args) {
		findDevices();
		establishConnection();
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
	
	private static void establishConnection() {
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
			config.setClientId("aaaaa-bbbbbb-cccccc-dddddd");
			config.setClientName("Nik");
			
			// Attempt ot initialise our Device object
			Device runningQuServerDevice = null;
			try {
				runningQuServerDevice = new Device(targetDevice, config);
			} catch (IOException e) {
				System.out.println("got IOException on Device constructor");
			} catch (RuntimeException e1) {
				System.out.println("got RuntimeException on Device constructor");
			} 
			
			// No problems so far, cross fingers and attempt to send a song!
			File audioFile = new File("TestAudiofiles/balls.mp3");
			runningQuServerDevice.uploadAudioFile(audioFile, FileFormat.MP3, "balls", "coolguy", "myalbum");
			
		} else {
			System.out.println("targetDevice is null!!!");
		}
	}
}
