package NetProbe;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class NetProbe {
	private String subnet;
	private String localAddress;
    private final ArrayList<ReachableQuDevice> availableDevices = new ArrayList<ReachableQuDevice>();

	public boolean initialise() {
		String localAddress = null;

        // Get the local address of this machine
		// TODO change this for the android specfic way to get ip.
		try {
            InetAddress ipAddr = InetAddress.getLocalHost();
            localAddress = ipAddr.getHostAddress();
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }
      
		// ------------ This code can be used on android to get the ip of the device when connected to wifi-------
		//        WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
		//        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
		//        int ip = wifiInfo.getIpAddress();
		//        String localAddress = Formatter.formatIpAddress(ip);
		// -------------------------------------------------------------------------------------------------------

		// Check to see if the host address is the loopback address, if so were
		// not connected to a network.
		if (localAddress.equals(C.LOCALHOST)) {
			return false;
		}

		// Strip the last byte of the local address to get subnet
		String[] localHostAddressBytes = localAddress.split("\\.");
		this.subnet = localHostAddressBytes[0] + "." + localHostAddressBytes[1] + "." + localHostAddressBytes[2];

		// Set the local address
		this.localAddress = localAddress;

		return true;
	}

	public ArrayList<ReachableQuDevice> getReachableQuDevices() {
        // List of all threads that have probes running on them
        ArrayList<Thread> probeThreads = new ArrayList<Thread>();

		// Clear the list of available devices
        synchronized(availableDevices) {
            availableDevices.clear();
        }

		// Iterate over each ip address in the subnet
		for (int i = 1; i < 255; i++) {
			// Construct the current address
			String currentAddress = subnet + '.' + i;
			
			// Make sure were not looking at address of this machine
			if(currentAddress.equals(localAddress)) {
				continue;
			}

            // Initialise a Probe and start on a new thread
            Probe probe = new Probe(this, currentAddress);
            Thread probeThread = new Thread(probe);
            probeThreads.add(probeThread);
            probeThread.start();
		}

        try {
           // Rejoin all probe threads
            for (Thread probeThread: probeThreads) {
                probeThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

		return availableDevices;
	}

    public void addReachableDevice(ReachableQuDevice device) {
        synchronized(availableDevices) {
            availableDevices.add(device);
        }
    }

	public String getSubnet() {
		return subnet;
	}
	public void setSubnet(String subnet) {
		this.subnet = subnet;
	}
}
