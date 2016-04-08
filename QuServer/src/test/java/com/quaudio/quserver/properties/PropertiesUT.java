package com.quaudio.quserver.properties;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import org.junit.Before;

public class PropertiesUT {
	private Properties properties;
	
	@Before
    public void setUp() {
		properties = new Properties();
    }
	
	@Test
    public void setAndGetDeviceID() {
		String newDeviceId = "test-device-id";
		// Set a new device id.
		properties.setDeviceId(newDeviceId);
		// Get the new device id.
		String deviceId = properties.getDeviceId();
		assertTrue("device id was not set correctly, expected'" + newDeviceId + "' but got '" + deviceId + "'",
				deviceId.equals(newDeviceId));
    }
	
	@Test
    public void setAndGetDeviceName() {
		String newDeviceName = "test-device-name";
		// Set a new device name.
		properties.setDeviceName(newDeviceName);
		// Get the new device name.
		String deviceName = properties.getDeviceName();
		assertTrue("device name was not set correctly, expected'" + newDeviceName + "' but got '" + deviceName + "'",
				deviceName.equals(newDeviceName));
    }
	
	@Test
    public void setAndGetDeviceAccessPassword() {
		String newDeviceAccessPassword = "test-device-access-password";
		// Set a new device access password.
		properties.setAccessPassword(newDeviceAccessPassword);
		// Get the new device access password.
		String deviceAccessPassword = properties.getAccessPassword();
		assertTrue("device access password was not set correctly, expected'" + newDeviceAccessPassword + "' but got '" + deviceAccessPassword + "'",
				deviceAccessPassword.equals(newDeviceAccessPassword));
    }
	
	@Test
    public void setAndGetDeviceSuperPassword() {
		String newDeviceSuperPassword = "test-device-super-password";
		// Set a new device super password.
		properties.setSuperPassword(newDeviceSuperPassword);
		// Get the new device super password.
		String deviceSuperPassword = properties.getSuperPassword();
		assertTrue("device super password was not set correctly, expected'" + newDeviceSuperPassword + "' but got '" + deviceSuperPassword + "'",
				deviceSuperPassword.equals(newDeviceSuperPassword));
    }
	
	@Test
    public void setAndGetUploadDirectory() {
		String newDeviceUploadDir = "test-device-upload-directory";
		// Set a new device upload directory.
		properties.setUploadDirectory(newDeviceUploadDir);
		// Get the new device upload directory.
		String deviceUploadDir = properties.getUploadDirectory();
		assertTrue("device upload directory was not set correctly, expected'" + newDeviceUploadDir + "' but got '" + deviceUploadDir + "'",
				deviceUploadDir.equals(newDeviceUploadDir));
	}
	
	@Test
    public void addAndGetSuperUsers() {
		// Create some test super user id's.
		String firstSuperUserId = "test-super-user-id-1";
		String secondSuperUserId = "test-super-user-id-2";
		String thirdSuperUserId = "test-super-user-id-3";
		// Add the super user id's to the properties file.
		properties.addSuperUser(firstSuperUserId);
		properties.addSuperUser(secondSuperUserId);
		properties.addSuperUser(thirdSuperUserId);
		// Get the super user id's and make sure the ones we added are present.
		ArrayList<String> superUserIds = properties.getSuperUsers();
		assertFalse("our list of super user id's should not be null", superUserIds == null);
		assertTrue("our list does not contain the first super user id", superUserIds.contains(firstSuperUserId));
		assertTrue("our list does not contain the second super user id", superUserIds.contains(secondSuperUserId));
		assertTrue("our list does not contain the third super user id", superUserIds.contains(thirdSuperUserId));
	}
	
	@Test
    public void addAndCheckSuperUser() {
		// Create a test super user id.
		String superUserId = "test-super-user-to-check";
		// We have not added this super user id, so 'isSuperUser()' should return false.
		assertFalse("we have not added this super user id so this should not return true", properties.isSuperUser(superUserId));
		// Add the super user id to the properties file.
		properties.addSuperUser(superUserId);
		// We have now added this super user id, so 'isSuperUser()' should return true.
		assertTrue("we have now added this super user id so this should return true", properties.isSuperUser(superUserId));
	}
	
	@Test
    public void addAndRemoveSuperUser() {
		// Create a test super user id.
		String superUserId = "test-super-user-to-remove";
		// Add the super user id to the properties file.
		properties.addSuperUser(superUserId);
		// Get the super user id's and make sure the one we added is present.
		ArrayList<String> superUserIds = properties.getSuperUsers();
		assertFalse("our list of super user id's should not be null", superUserIds == null);
		assertTrue("our list does not contain our super user id", superUserIds.contains(superUserId));
		// Remove the super user id.
		properties.removeSuperUser(superUserId);
		// Get the list of super user id's.
		superUserIds = properties.getSuperUsers();
		// Check to make sure that our super user id is no longer included.
		assertFalse("our list of super user id's should not be null", superUserIds == null);
		assertFalse("our list should not contain the super user id we removed", superUserIds.contains(superUserId));
	}
}
