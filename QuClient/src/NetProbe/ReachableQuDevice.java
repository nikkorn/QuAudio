package NetProbe;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class ReachableQuDevice {
	private String deviceId;
	private String deviceName;
	private String address;
	private int afrPort;
	private int cmPort;
	private boolean isProtected;
	
	public ReachableQuDevice(String deviceId, String deviceName, int afrPort, int cmPort, String address, boolean isProtected) {
		this.deviceId = deviceId;
		this.address = address;
		this.isProtected = isProtected;
		this.deviceName = deviceName;
		this.afrPort = afrPort;
		this.cmPort = cmPort;
	}
	
	public String getDeviceId() {
		return deviceId;
	}

	public String getAddress() {
		return address;
	}

	public boolean isProtected() {
		return isProtected;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public int getAudioFileReceiverPort() {
		return afrPort;
	}

	public int getClientManagerPort() {
		return cmPort;
	}
}
