package UnitTests;

import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.Test;
import Config.ClientConnectionConfig;
import NetProbe.NetProbe;
import NetProbe.ReachableQuDevice;
import Server.Device;

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
			// We should have at least one (local QuServer) instance running.
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
		// Attempt to get local Qu server instance
		if(probe.initialise(null)) {
			ArrayList<ReachableQuDevice> devices = probe.getReachableQuDevices(true);
			for(ReachableQuDevice device : devices) {
				if(device.getAddress().equals("127.0.0.1")) {
					// We will be using the local instance on Qu server for this test
					targetDevice = device;
					break;
				}
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
			
			// Attempt to initialise our Device object.
			Device runningQuServerDevice = null;
			try {
				runningQuServerDevice = new Device(targetDevice, config);
			} catch (IOException e) {
				fail("got IOException on attempting to create our Device object");
			} catch (RuntimeException e1) {
				fail("got Runtimeexception on attempting to create our Device object");
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

	
	
	
	
	
	
	
}
