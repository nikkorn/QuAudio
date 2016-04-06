package com.quaudio.quserver.server;

import org.junit.Test;
import static org.junit.Assert.*;

public class UtilsUT {
	
	@Test
    public void setVolumeMid() {
		// System volume should be in range on 0 - 100.
		int midVolume = 50;
		Utils.getUtils().setMasterVolume(midVolume);
		assertTrue("The master volume was not set correctly", Utils.getUtils().getMasterVolume() == midVolume);
    }
	
	@Test
    public void setVolumeMin() {
		// Reset the volume.
		int volume = 50;
		Utils.getUtils().setMasterVolume(volume);
		assertTrue("The master volume was not set correctly", Utils.getUtils().getMasterVolume() == volume);
		// Now set the volume to be at the minimum.
		volume = 0;
		Utils.getUtils().setMasterVolume(volume);
		assertTrue("The master volume was not set correctly when set to the minimum", Utils.getUtils().getMasterVolume() == volume);
    }
	
	@Test
    public void setVolumeMax() {
		// Reset the volume.
		int volume = 50;
		Utils.getUtils().setMasterVolume(volume);
		assertTrue("The master volume was not set correctly", Utils.getUtils().getMasterVolume() == volume);
		// Now set the volume to be at the maximum.
		volume = 100;
		Utils.getUtils().setMasterVolume(volume);
		assertTrue("The master volume was not set correctly when set to the maximum", Utils.getUtils().getMasterVolume() == volume);
    }
	
	@Test
    public void setVolumeBelowMin() {
		// Reset the volume.
		int volume = 50;
		Utils.getUtils().setMasterVolume(volume);
		assertTrue("The master volume was not set correctly", Utils.getUtils().getMasterVolume() == volume);
		// Now set the volume to be 1 below the minimum.
		Utils.getUtils().setMasterVolume(-1);
		assertTrue("Setting the master volume to be below 0 should result in the volume staying the same", Utils.getUtils().getMasterVolume() == volume);
    }
	
	@Test
    public void setVolumeAboveMax() {
		// Reset the volume.
		int volume = 50;
		Utils.getUtils().setMasterVolume(volume);
		assertTrue("The master volume was not set correctly", Utils.getUtils().getMasterVolume() == volume);
		// Now set the volume to be 1 above the maximum.
		Utils.getUtils().setMasterVolume(101);
		assertTrue("Setting the master volume to be above 100 should result in the volume staying the same", Utils.getUtils().getMasterVolume() == volume);
    }
}
