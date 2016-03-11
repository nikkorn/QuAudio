package quclient.NetProbe;

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

    /**
     * Initialises the NetProbe
     * @param Address of this machine on the network, if null then we will attempt to deduce it.
     * @return success
     */
    public boolean initialise(String address) {
        // Check that we weren't given a null address, if we have then we are expected to deduce the address.
        if(address == null) {
             // Get the local address of this machine
            try {
                InetAddress ipAddr = InetAddress.getLocalHost();
                address = ipAddr.getHostAddress();
            } catch (UnknownHostException ex) {
                // We failed to get address, return false.
                return false;
            }
        }
        // Check to see if the host address is the loopback address, if so we're not connected to a network.
        if (address.equals(C.LOCALHOST)) {
            return false;
        }
        // Strip the last byte of the local address to get subnet
        String[] localHostAddressBytes = address.split("\\.");
        // Check that we have 4 values (valid *.*.*.* layout for IPv4)
        if(localHostAddressBytes.length != 4) {
            // Wrong number of byte values.
            return false;
        }
        this.subnet = localHostAddressBytes[0] + "." + localHostAddressBytes[1] + "." + localHostAddressBytes[2];
        // Set the local address
        this.localAddress = address;
        // It seems that everything went well, return true.
        return true;
    }

    /**
     * Uses a multithreaded solution to probe for any available servers on the local network.
     * @param runLocally If true the local machine is included in the search for an available server.
     * @return A list of available devices
     */
    public ArrayList<ReachableQuDevice> getReachableQuDevices(boolean runLocally) {
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
        // If the user has requested to run the client on the same machine then we include loopback
        if(runLocally) {
            // Initialise a Probe to search for a running server locally and start on a new thread
            Probe probe = new Probe(this, C.LOCALHOST);
            Thread probeThread = new Thread(probe);
            probeThreads.add(probeThread);
            probeThread.start();
        }
        // Rejoin all probe threads.
        try {
            for (Thread probeThread: probeThreads) {
                probeThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Return all of the available devices in the form of ReachableQuDevice objects.
        return availableDevices;
    }

    /**
     * Called by a Probe object on finding a running QuServer instance.
     * @param device
     */
    public void addReachableDevice(ReachableQuDevice device) {
        synchronized(availableDevices) {
            availableDevices.add(device);
        }
    }

    /**
     * Get the subnet. 
     * @return subnet
     */
    public String getSubnet() {
        return subnet;
    }

    /**
     * set the subnet
     * @param subnet
     */
    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }
}
