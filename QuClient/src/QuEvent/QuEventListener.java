package QuEvent;

/**
 * An observable for listening for Device state changes.
 * @author Nikolas Howard 
 *
 */
public interface QuEventListener {
	
	/**
	 * Called when the Device settings are updated.
	 */
	public void onQuSettingsUpdate();
	
	/**
	 * Called when the Device PlayList is updated.
	 */
	public void onQuPlayListUpdate();
	
	/**
	 * Called when the connection to the QuServer is broken.
	 */
	public void quQuDisconnect();
}
