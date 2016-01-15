package quclient.QuEvent;

import quclient.Server.Device;

/**
 * An observer for listening for Device state changes.
 * @author Nikolas Howard 
 *
 */
public interface QuEventListener {
	
	/**
	 * Called when the Device settings are updated.
	 * @param sourceDevice
	 */
	public void onQuSettingsUpdate(Device sourceDevice);
	
	/**
	 * Called when the Device PlayList is updated.
	 * @param sourceDevice
	 */
	public void onQuPlayListUpdate(Device sourceDevice);
	
	/**
	 * Called when the Device master volume is updated.
	 * @param sourceDevice
	 */
	public void onQuMasterVolumeUpdate(Device sourceDevice);
	/**
	 * Called when the connection to the QuServer is broken.
	 * @param sourceDevice
	 */
	public void quQuDisconnect(Device sourceDevice);
}
