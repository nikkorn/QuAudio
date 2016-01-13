package Server;

import java.io.IOException;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class Utils {
	
	//-------------------- Handle System Volume ------------------------
	private static int masterVolumeLevel = 60;
	
	/**
	 * Set the systems master volume
	 * @param level
	 */
	public static void setMasterVolume(int level) {
		// Check that the new level is within bounds 0-100.
		if(level < 0 || level > 100) {
			// This is not a valid level
			Log.log(Log.MessageType.ERROR, "UTILS", "the master volume level must be in the range 0 - 100");
			return;
		} else {
			// Set the master volume level.
			masterVolumeLevel = level;
		}
		// Behave differently based on OS.
		if(Server.properties.isOSUnixLike()) {
			// Attempt to set the volume for OS using channel Master
			try {
				Runtime.getRuntime().exec("amixer set Master " + level);
			} catch(IOException e) {}
			// Attempt to set the volume for OS using channel PCM (Raspberry Pi)
			try {
				Runtime.getRuntime().exec("amixer set PCM -- " + level + "%");
			} catch(IOException e) {}
			Log.log(Log.MessageType.ERROR, "UTILS", "set system master volume to '" + level + "'");
		} else {
			// At the moment we have no support for changing volume for a non-Unix like OS.
			Log.log(Log.MessageType.ERROR, "UTILS", "setting master volume is currently only supported on Unix like OS");
		}
	}
	
	/**
	 * Get the systems master volume
	 * @return
	 */
	public static int getMasterVolume() {
		return masterVolumeLevel;
	}
	//------------------------------------------------------------------
	
}