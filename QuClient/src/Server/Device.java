package Server;

import NetProbe.ReachableQuDevice;

/**
 * Represents an instance of a Qu Server
 * @author Nikolas Howard
 *
 */
public class Device {
	private ReachableQuDevice reachableDevice;
	
	public Device(ReachableQuDevice reachableDevice) {
		this.reachableDevice = reachableDevice;
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
}
