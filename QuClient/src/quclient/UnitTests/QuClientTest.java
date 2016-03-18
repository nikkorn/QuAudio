package quclient.UnitTests;

import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;
import org.junit.Test;
import quclient.Config.ClientConnectionConfig;
import quclient.FileTransfer.FileFormat;
import quclient.NetProbe.NetProbe;
import quclient.NetProbe.ReachableQuDevice;
import quclient.ProxyPlaylist.PlayList;
import quclient.ProxyPlaylist.TrackState;
import quclient.QuEvent.QuEventListener;
import quclient.Server.Device;
import quclient.Server.HandshakeResponse;

/**
 * Various tests for the QuClient API Library. A local (and up-to-date) instance of the QuServer MUST be running. 
 * @author Nikolas Howard
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class QuClientTest {

	/**
	 * Test that we can find our locally running QuServer instance.
	 */
	@Test
	public void t1_FindLocalServer() {
		NetProbe probe = new NetProbe();
		if(probe.initialise(null)) {
			// Wait and get our results.
			ArrayList<ReachableQuDevice> devices = probe.getReachableQuDevices(true);
			// We should have at least one server instance running.
			if(devices.size() == 0) {
				fail("we should at least have got our locally running QuServer instance. Is it running?");
			}
		} else {
			fail("we failed to intialise our NetProbe");
		}
	}
	
	/**
	 * Test that we can instantiate a Device object and connect/disconnect from local QuServer instance.
	 */
	@Test
	public void t2_ConnectAndDisconnect() {
		NetProbe probe = new NetProbe();
		ReachableQuDevice targetDevice = null;
		// Attempt to get Qu server instance
		if(probe.initialise(null)) {
			ArrayList<ReachableQuDevice> devices = probe.getReachableQuDevices(true);
			for(ReachableQuDevice device : devices) {
				targetDevice = device;
				break;
			}
		} else {
			fail("we failed to intialise our NetProbe");
		}
		// Check that we actually got a ReachableQuDevice
		if(targetDevice != null) {
			// Create a Client Config
			ClientConnectionConfig config = new ClientConnectionConfig();
			config.setClientId(UUID.randomUUID().toString()); // Generate a random id so we can connect multiple clients.
			config.setClientName("Nik");
			config.setAccessPassword("a1a1");
			
			// Attempt to initialise our Device object.
			Device runningQuServerDevice = new Device();
			try {
				runningQuServerDevice.link(targetDevice, config);
			} catch (RuntimeException e1) {
				fail("got RuntimeException on attempting to link our Device object");
			} 
			
			// Make sure that we are connected.
			assertTrue("we are not connected", runningQuServerDevice.isConnected());
			
			// Disconnect from local QuServer instance.
			runningQuServerDevice.disconnect();
		
			// Make sure that we are not connected anymore.
			assertFalse("we are still connected after calling disconnect()", runningQuServerDevice.isConnected());
		} else {
			fail("getReachableQuDevices() returned nothing or null value");
		}
	}	
	
	/**
	 * Test that we can instantiate a Device object, update the server settings remotely, and get
	 * a message from the server notifying us of this change.
	 */
	@Test
	public void t3_UpdateAndGetServerSettings() {
		NetProbe probe = new NetProbe();
		ReachableQuDevice targetDevice = null;
		// Attempt to get Qu server instance
		if(probe.initialise(null)) {
			ArrayList<ReachableQuDevice> devices = probe.getReachableQuDevices(true);
			for(ReachableQuDevice device : devices) {
				targetDevice = device;
				break;
			}
		} else {
			fail("we failed to intialise our NetProbe");
		}
		// Check that we actually got a ReachableQuDevice
		if(targetDevice != null) {
			// Create a Client Config
			ClientConnectionConfig config = new ClientConnectionConfig();
			config.setClientId("test_super_client_id"); // We need to use a client id that the server recognises as the id of a super user (needed for settings update)
			config.setClientName("Nik");
			config.setAccessPassword("a1a1");
			
			// The name that we will be temporarily changing the server to for the test.
			String newServerName = "New-Server-Name";
			// A list to hold the changing server names.
			LinkedList<String> serverNamesList = new LinkedList<String>();
			
			// Attempt to initialise our Device object.
			Device runningQuServerDevice = new Device();
			
			// Add an event listener to listen for settings update.
			runningQuServerDevice.addQuEventListener(new QuEventListener() {
				@Override
				public void onQuSettingsUpdate(Device sourceDevice) {
					// Add the updated server name value.
					serverNamesList.add(sourceDevice.getDeviceName());
				}
				@Override
				public void onQuPlayListUpdate(Device sourceDevice) {
				}
				@Override
				public void onQuMasterVolumeUpdate(Device sourceDevice) {
				}
				@Override
				public void onQuDisconnect(Device sourceDevice) {
				}
				@Override
				public void onQuConnect(Device sourceDevice) {
				}
				@Override
				public void onQuLinkFailure(ReachableQuDevice device, HandshakeResponse handshakeResponse) {
				}
			});
						
			try {
				runningQuServerDevice.link(targetDevice, config);
			} catch (RuntimeException e1) {
				fail("got RuntimeException on attempting to link our Device object");
			} 
			
			// Get the original unchanged server name.
			String originalServerName = runningQuServerDevice.getDeviceName();
			serverNamesList.add(originalServerName);
			
			// Change the server name, leaving access password blank.
			runningQuServerDevice.updateQuServerSettings(newServerName, null, null);
			
			// Sleep for a bit to give the server time to process the settings update, send it back, and our listener get the event.
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// That should be long enough, change the server name back now, still leaving access password blank.
			runningQuServerDevice.updateQuServerSettings(originalServerName, null, null);
			
			// Sleep for a bit to give the server time to process the settings update, send it back, and our listener get the event.
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// We should have a list containing the original name, the updated name, and the original name again as we set it back.
			assertTrue("should have original name, new name, original name. we only have " + serverNamesList.size()  + " value(s).", serverNamesList.size() == 3);
			// The first name (the original) and the last name (the reverted name) should be the same
			assertTrue("original and reverted server name should be the same", serverNamesList.get(0).equals(serverNamesList.get(2)));
			// The second server name (updated name) should not be the same as the original name.
			assertFalse("original and updated server name should not be the same", serverNamesList.get(0).equals(serverNamesList.get(1)));
						
			// Disconnect
			runningQuServerDevice.disconnect();
		} else {
			fail("getReachableQuDevices() returned nothing or null value");
		}
	}
	
	/**
	 * Our device PlayList.
	 */
	PlayList currentPlaylist = null;
	/**
	 * Test that we can upload a track, that it plays, that we can pause it, that we can play it again, and that we can stop it.
	 */
	@Test
	public void t4_UploadAndManipulateTrack() {
		NetProbe probe = new NetProbe();
		ReachableQuDevice targetDevice = null;
		// Attempt to get Qu server instance
		if(probe.initialise(null)) {
			ArrayList<ReachableQuDevice> devices = probe.getReachableQuDevices(true);
			for(ReachableQuDevice device : devices) {
				targetDevice = device;
				break;
			}
		} else {
			fail("we failed to intialise our NetProbe");
		}
		// Check that we actually got a ReachableQuDevice
		if(targetDevice != null) {
			// Create a Client Config
			ClientConnectionConfig config = new ClientConnectionConfig();
			config.setClientId(UUID.randomUUID().toString()); // Generate a random id so we can connect multiple clients.
			config.setClientName("Nik");
			config.setAccessPassword("a1a1");
			
			// Attempt to initialise our Device object.
			Device runningQuServerDevice = new Device();
			
			// Add an event listener to listen for PlayList updates.
			runningQuServerDevice.addQuEventListener(new QuEventListener() {
				@Override
				public void onQuSettingsUpdate(Device sourceDevice) {
				}
				@Override
				public void onQuPlayListUpdate(Device sourceDevice) {
					// Update our reference to point to the newest PlayList.
					currentPlaylist = sourceDevice.getPlayList();
				}
				@Override
				public void onQuMasterVolumeUpdate(Device sourceDevice) {
				}
				@Override
				public void onQuDisconnect(Device sourceDevice) {
				}
				@Override
				public void onQuConnect(Device sourceDevice) {
				}
				@Override
				public void onQuLinkFailure(ReachableQuDevice device, HandshakeResponse handshakeResponse) {
				}
			});
			
			// Attempt to connect to server.
			try {
				runningQuServerDevice.link(targetDevice, config);
			} catch (RuntimeException e1) {
				fail("got RuntimeException on attempting to link our Device object");
			} 
			
			// Get our latest PlayList.
			currentPlaylist = runningQuServerDevice.getPlayList();
			
			// Nothing is uploaded, check that we have an empty PlayList.
			assertTrue("we should not have any tracks in our playlist as nothing has been uploaded", currentPlaylist.getTracks().size() == 0);
			
			// Upload a track to the QuServer.
			File audioFile = new File("TestAudioFiles/balls.mp3");
			runningQuServerDevice.uploadAudioFile(audioFile, FileFormat.MP3, "balls_of_fire", "cool_guy", "my_album");
			
			// Sleep for a bit to give the server time to read the track into a file and start playing it. And for the 
			// QuServer to notify connected clients of the PlayList state change.
			try {
				Thread.sleep(3500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Our QuServer should have our track and hopefully it should be playing. Our anonymous QuEventListener should 
			// have also picked up a QuEvent that was fired after the QuServer notified the client with details of the change
			// and the newest state of the PlayList. First, check that the PlayList was updated and that there is a Track.
			assertTrue("we should have the one track we uploaded in our PlayList", currentPlaylist.getTracks().size() == 1);
			// Check that the TrackState of this file is PLAYING.
			assertTrue("the TrackState of this track should be PLAYING", currentPlaylist.getTracks().get(0).getTrackState() == TrackState.PLAYING);
			
			// Allow the track to play for a couple of seconds.
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Pause our track.
			currentPlaylist.getTracks().get(0).pause();
			
			// Allow time for this to be processed.
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
						
			// We hopefully received acknowledgements from the QuServer in the form of a PUSH_PLAYLIST IncomingAction. 
			// And our local PlayList should have been updated by our QuEventListener. Check we have a track with a PAUSED state.
			assertTrue("the TrackState of this track should be PAUSED", currentPlaylist.getTracks().get(0).getTrackState() == TrackState.PAUSED);
			
			// Allow the track to stay paused for a couple of seconds.
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Play our track again.
			currentPlaylist.getTracks().get(0).play();
			
			// Allow time for this to be processed.
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// We hopefully received acknowledgements from the QuServer in the form of a PUSH_PLAYLIST IncomingAction. 
			// And our local PlayList should have been updated by our QuEventListener. Check we have a track with a PLAYING state.
			assertTrue("the TrackState of this track should be PLAYING as we unpaused it", currentPlaylist.getTracks().get(0).getTrackState() == TrackState.PLAYING);
			
			// Allow time for this to play before we stop it.
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Play our track again.
			currentPlaylist.getTracks().get(0).stop();
			
			// Allow time for this to be processed.
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// The only song in the PlayList should have been stopped, and the client should have been updated.
			// Check that our most recent PlayList is completely empty.
			assertTrue("we stopped our only track, and therefore we should have no tracks in our PlayList", currentPlaylist.getTracks().size() == 0);
			
			// Disconnect
			runningQuServerDevice.disconnect();
		} else {
			fail("getReachableQuDevices() returned nothing or null value");
		}
	}
	
	/**
	 * Test that we can upload two tracks, and that when we stop the first the second will begin to play automatically.
	 */
	@Test
	public void t5_UploadStopAndSkip() {
		NetProbe probe = new NetProbe();
		ReachableQuDevice targetDevice = null;
		// Attempt to get Qu server instance
		if(probe.initialise(null)) {
			ArrayList<ReachableQuDevice> devices = probe.getReachableQuDevices(true);
			for(ReachableQuDevice device : devices) {
				targetDevice = device;
				break;
			}
		} else {
			fail("we failed to intialise our NetProbe");
		}
		// Check that we actually got a ReachableQuDevice
		if(targetDevice != null) {
			// Create a Client Config
			ClientConnectionConfig config = new ClientConnectionConfig();
			config.setClientId(UUID.randomUUID().toString()); // Generate a random id so we can connect multiple clients.
			config.setClientName("Nik");
			config.setAccessPassword("a1a1");
			
			// Attempt to initialise our Device object.
			Device runningQuServerDevice = new Device();
			
			// Add an event listener to listen for PlayList updates.
			runningQuServerDevice.addQuEventListener(new QuEventListener() {
				@Override
				public void onQuSettingsUpdate(Device sourceDevice) {
				}
				@Override
				public void onQuPlayListUpdate(Device sourceDevice) {
					// Update our reference to point to the newest PlayList.
					currentPlaylist = sourceDevice.getPlayList();
				}
				@Override
				public void onQuMasterVolumeUpdate(Device sourceDevice) {
				}
				@Override
				public void onQuDisconnect(Device sourceDevice) {
				}
				@Override
				public void onQuConnect(Device sourceDevice) {
				}
				@Override
				public void onQuLinkFailure(ReachableQuDevice device, HandshakeResponse handshakeResponse) {
				}
			});
			
			// Attempt to connect to server.
			try {
				runningQuServerDevice.link(targetDevice, config);
			} catch (RuntimeException e1) {
				fail("got RuntimeException on attempting to link our Device object");
			} 
			
			// Get our latest PlayList.
			currentPlaylist = runningQuServerDevice.getPlayList();
			
			// Nothing is uploaded, check that we have an empty PlayList.
			assertTrue("we should not have any tracks in our playlist as nothing has been uploaded", currentPlaylist.getTracks().size() == 0);
			
			// Upload a track to the QuServer.
			File audioFile = new File("TestAudioFiles/balls.mp3");
			runningQuServerDevice.uploadAudioFile(audioFile, FileFormat.MP3, "balls_of_fire", "cool_guy", "my_album");
			
			File audioFile2 = new File("TestAudioFiles/classical.mp3");
			runningQuServerDevice.uploadAudioFile(audioFile2, FileFormat.MP3, "classical", "old_guy", "classical_album");
			
			// Sleep for a bit to give the server time to read the track into a file and start playing it. And for the 
			// QuServer to notify connected clients of the PlayList state change.
			try {
				Thread.sleep(3500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// We should have two tracks in our playlist.
			assertTrue("we should have two tracks uploaded in our PlayList", currentPlaylist.getTracks().size() == 2);
			// Check that the TrackState of the first file is PLAYING.
			assertTrue("the TrackState of the first track should be PLAYING", currentPlaylist.getTracks().get(0).getTrackState() == TrackState.PLAYING);
			// Check that the TrackState of the second file is PENDING.
			assertTrue("the TrackState of the second track should be PENDING", currentPlaylist.getTracks().get(1).getTrackState() == TrackState.PENDING);
			
			
			// Allow the track to play for a couple of seconds.
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Stop our first track our track.
			currentPlaylist.getTracks().get(0).stop();
			
			// Allow time for this to be processed.
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
						
			// We should now have only one track in our playlist as the first was stopped.
			assertTrue("We should now have only one track in our playlist as the first was stopped", currentPlaylist.getTracks().size() == 1);
			// Check that the TrackState of the first file is PLAYING.
			assertTrue("the TrackState of the first track should be PLAYING", currentPlaylist.getTracks().get(0).getTrackState() == TrackState.PLAYING);
		
			
			// Allow the second track to play for a few of seconds
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// stop this track too.
			currentPlaylist.getTracks().get(0).stop();
			
			// Allow time for this to be processed.
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// We should now have only one track in our playlist as the first was stopped.
			assertTrue("We should now have no tracks in the PlayList", currentPlaylist.getTracks().size() == 0);
			
			// Allow time before disconnecting
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Disconnect
			runningQuServerDevice.disconnect();
		} else {
			fail("getReachableQuDevices() returned nothing or null value");
		}
	}
	
	/**
	 * Test that we can upload three tracks, and that when we remove the second track we are left with the other two in order.
	 * We will then attempt to remove the currently playing track.
	 */
	@Test
	public void t6_UploadAndRemove() {
		NetProbe probe = new NetProbe();
		ReachableQuDevice targetDevice = null;
		// Attempt to get local Qu server instance
		if(probe.initialise(null)) {
			ArrayList<ReachableQuDevice> devices = probe.getReachableQuDevices(true);
			for(ReachableQuDevice device : devices) {
				targetDevice = device;
				break;
			}
		} else {
			fail("we failed to intialise our NetProbe");
		}
		// Check that we actually got a ReachableQuDevice
		if(targetDevice != null) {
			// Create a Client Config
			ClientConnectionConfig config = new ClientConnectionConfig();
			config.setClientId(UUID.randomUUID().toString()); // Generate a random id so we can connect multiple clients.
			config.setClientName("Nik");
			config.setAccessPassword("a1a1");
			
			// Attempt to initialise our Device object.
			Device runningQuServerDevice = new Device();
			
			// Add an event listener to listen for PlayList updates.
			runningQuServerDevice.addQuEventListener(new QuEventListener() {
				@Override
				public void onQuSettingsUpdate(Device sourceDevice) {
				}
				@Override
				public void onQuPlayListUpdate(Device sourceDevice) {
					// Update our reference to point to the newest PlayList.
					currentPlaylist = sourceDevice.getPlayList();
				}
				@Override
				public void onQuMasterVolumeUpdate(Device sourceDevice) {
				}
				@Override
				public void onQuDisconnect(Device sourceDevice) {
				}
				@Override
				public void onQuConnect(Device sourceDevice) {
				}
				@Override
				public void onQuLinkFailure(ReachableQuDevice device, HandshakeResponse handshakeResponse) {
				}
			});
			
			// Attempt to connect to server.
			try {
				runningQuServerDevice.link(targetDevice, config);
			} catch (RuntimeException e1) {
				fail("got RuntimeException on attempting to link our Device object");
			} 
			
			// Get our latest PlayList.
			currentPlaylist = runningQuServerDevice.getPlayList();
			
			// Nothing is uploaded, check that we have an empty PlayList.
			assertTrue("we should not have any tracks in our playlist as nothing has been uploaded", currentPlaylist.getTracks().size() == 0);
			
			// Upload a track to the QuServer.
			File audioFile = new File("TestAudioFiles/balls.mp3");
			runningQuServerDevice.uploadAudioFile(audioFile, FileFormat.MP3, "balls_of_fire", "cool_guy", "my_album");
			
			// Sleep for a bit, we want these tracks to be uploaded in order.
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Upload a second track (the one we will remove first).
			File audioFile2 = new File("TestAudioFiles/classical.mp3");
			runningQuServerDevice.uploadAudioFile(audioFile2, FileFormat.MP3, "classical", "old_guy", "classical_album");
			
			// Sleep for a bit, we want these tracks to be uploaded in order.
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Upload a third track.
			File audioFile3 = new File("TestAudioFiles/balls.mp3");
			runningQuServerDevice.uploadAudioFile(audioFile3, FileFormat.MP3, "balls_of_fire_2", "cool_guy", "my_album");
			
			// Sleep for a bit to give the server time to read the track into a file and start playing it. And for the 
			// QuServer to notify connected clients of the PlayList state change.
			try {
				Thread.sleep(3500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// We should have three tracks in our playlist.
			assertTrue("we should have three tracks uploaded in our PlayList", currentPlaylist.getTracks().size() == 3);
			// Check that the TrackState of the first file is PLAYING.
			assertTrue("the TrackState of the first track should be PLAYING", currentPlaylist.getTracks().get(0).getTrackState() == TrackState.PLAYING);
			
			// Allow the track to play for a couple of seconds.
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Remove our second track.
			currentPlaylist.getTracks().get(1).remove();
			
			// Allow time for this to be processed.
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
						
			// We should now have two tracks in our PlayList.
			assertTrue("We should now only have two tracks in our PlayList", currentPlaylist.getTracks().size() == 2);
			// Check that the TrackState of the first file is PLAYING.
			assertTrue("the TrackState of the first track should still be PLAYING", currentPlaylist.getTracks().get(0).getTrackState() == TrackState.PLAYING);
			// Check that the first track in the PlayList is the first track we uploaded.
			assertTrue("The first track in the PlayList should be the first track we uploaded", currentPlaylist.getTracks().get(0).getName().equals("balls_of_fire"));
			// Check that the second track in the PlayList is the third track we uploaded.
			assertTrue("The second track in the PlayList should be the third track we uploaded", currentPlaylist.getTracks().get(1).getName().equals("balls_of_fire_2"));
		
			// Allow the second track to play for a few of seconds
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Remove the currently playing track.
			currentPlaylist.getTracks().get(0).remove();
			
			// Allow time for this to be processed.
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// We should now have only one track in our playlist as the first was stopped.
			assertTrue("We should now have only one track in the PlayList", currentPlaylist.getTracks().size() == 1);
			
			// Remove the current (and last) playing track.
			currentPlaylist.getTracks().get(0).remove();
			
			// Wait a bit before we disconnect.
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Disconnect
			runningQuServerDevice.disconnect();
		} else {
			fail("getReachableQuDevices() returned nothing or null value");
		}
	}
}
