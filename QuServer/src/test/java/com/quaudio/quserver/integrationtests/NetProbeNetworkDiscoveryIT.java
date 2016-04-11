package com.quaudio.quserver.integrationtests;

import static org.junit.Assert.*;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.quaudio.quclient.netprobe.C;
import com.quaudio.quclient.netprobe.NetProbe;
import com.quaudio.quclient.netprobe.ReachableQuDevice;
import com.quaudio.quserver.netbeacon.Beacon;
import com.quaudio.quserver.properties.Properties;
import com.quaudio.quserver.server.Server;

public class NetProbeNetworkDiscoveryIT {
	private Beacon testListeningBeacon = null;
	
	@Before
	public void initialiseBeacon() {
		// Need to initialise our servers properties object in order for our beacon
		// to be able to supply information relating to the server.
		Server.properties = new Properties();
		// Initialise our Beacon.
		testListeningBeacon = new Beacon(C.PROBE_PORT, C.PROBE_CLIENT_RECEIVER_PORT);
		testListeningBeacon.start();
	}
	
	/**
	 * Test that we can find our local beacon.
	 */
	@Test
	public void discoverBeacon() {
		NetProbe probe = new NetProbe();
		// Wait and get our results.
		ArrayList<ReachableQuDevice> devices = probe.getReachableQuDevices(true);
		// We should have received a response from our beacon.
		assertTrue("We failed to get a response from our beacon", devices.size() > 0);
	}
	
	@After
	public void stopBeacon() {
		// Need to stop the beacon we initialised.
		testListeningBeacon.stop();
	}
}
