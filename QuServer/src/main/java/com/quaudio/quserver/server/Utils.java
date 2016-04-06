package com.quaudio.quserver.server;

import java.io.IOException;
import com.quaudio.quserver.quinterface.LEDBehaviour;
import com.quaudio.quserver.quinterface.LEDColourDefault;
import com.quaudio.quserver.quinterface.QuInterface;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class Utils {
	
	private static Utils utils = null;
	
	/**
	 * Use singleton pattern to provide single instance of Utils.
	 * @return
	 */
	public static Utils getUtils() {
		if(utils == null) {
			utils = new Utils();
		} 
		return utils;
	}
	
	//-------------------- Handle System Volume ------------------------
	private int masterVolumeLevel = 60;
	
	/**
	 * Set the systems master volume
	 * @param level
	 */
	public void setMasterVolume(int level) {
		// Check that the new level is within bounds 0-100.
		if(level < 0 || level > 100) {
			// This is not a valid level
			Log.log(Log.MessageType.ERROR, "UTILS", "the master volume level must be in the range 0 - 100");
			return;
		} else {
			// Set the master volume level.
			masterVolumeLevel = level;
		}
		// Check that the server properties file has been initialised.
		if(Server.properties != null) {
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
		} else {
			// Properties have not been properly initialised, this may be due to this method being
			// run in the context of a unit/integration test.
			Log.log(Log.MessageType.WARNING, "UTILS", "Server properties not initalised, cannot apply change of volume");
		}
	}
	
	/**
	 * Get the systems master volume
	 * @return
	 */
	public int getMasterVolume() {
		return masterVolumeLevel;
	}
	
	//------------------------------------------------------------------
	
	//----------------------------- Power ------------------------------
	
	public void shutdownDevice(boolean isOSUnixLike) {
		// Interact with the QuInterface to set a standby (red) light
		QuInterface.show(LEDColourDefault.RED, LEDBehaviour.GLOW);
		// Shutdown the device (only if Unix like OS)
		if(isOSUnixLike) {
			try {
				Runtime.getRuntime().exec("sudo shutdown -h now");
			} catch (IOException e) {
				Log.log(Log.MessageType.ERROR, "SERVER", "failed to shutdown");
			}
		}
	}
	
	//------------------------------------------------------------------
}
