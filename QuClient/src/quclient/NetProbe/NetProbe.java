package quclient.NetProbe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class NetProbe {
    private final ArrayList<ReachableQuDevice> availableDevices = new ArrayList<ReachableQuDevice>();

    /**
     * Get any available servers on the local network.
     * @param runLocally If true the local machine is included in the search for an available server.
     * @return A list of available devices
     */
    public ArrayList<ReachableQuDevice> getReachableQuDevices(boolean runLocally) {
        // Clear the list of available devices
        availableDevices.clear();
        //In a separate thread, send a probe and listen for responses.
        Thread probeThread = new Thread(new Runnable() {
        	
			@Override
			public void run() {
				// A list that temporarily stores the addresses of any servers that respond.
		        ArrayList<String> respondingServerAddresses = new ArrayList<String>();
		        // Create our DatagramSocket on which to send a probe and listen for reponses.
		        DatagramSocket probeSocket = null;
		        
		        try {
		        	// Initialise our DatagramSocket.
					probeSocket = new DatagramSocket();
					probeSocket.setBroadcast(true);
					// Send our probe
					byte[] probeMessage = "QU_C_PRB".getBytes();
					try {
						DatagramPacket probe = new DatagramPacket(probeMessage, probeMessage.length, InetAddress.getByName("255.255.255.255"), C.PROBE_PORT);
						probeSocket.send(probe);
					} catch (Exception e) {
						// Error sending probe.
						e.printStackTrace();
					} 
					
					// Now wait for server responses.
					probeSocket.setSoTimeout(1000);  
					boolean listening = true;
					while(listening) {
					    try {
					        byte[] responseBuffer = new byte[15000];  
					        DatagramPacket possibleResponse = new DatagramPacket(responseBuffer, responseBuffer.length);  
							probeSocket.receive(possibleResponse);
					        // We have got a packet! is it a valid response?
					        String response = new String(possibleResponse.getData());  
					        if(response != null && response.trim().equals("QU_S_RSP")) {
					        	// This is a valid response!
					        	respondingServerAddresses.add(possibleResponse.getAddress().getHostAddress());
					        }
					    } catch(SocketTimeoutException ste) {
					        // If another server hasn't responded by now, then we can assume there are no more.
					    	listening = false;
					    } catch (IOException e) {} 
					}
				} catch (SocketException e) {
					e.printStackTrace();
				} finally {
					// Always close the socket.
					if(probeSocket != null) {
						probeSocket.close();
					}
				}
		        
		        // We have given enough time for any servers to respond. Now visit each server found and get details from it.
		        for(String serverAddress : respondingServerAddresses) {
		        	ReachableQuDevice device = createReachableQuDeviceObject(serverAddress);
		        	if(device != null) {
		        		// Add the device to our list of reachable qu devices.
		        		addReachableDevice(device);
		        	}
		        }
			}
			
        });
        probeThread.setDaemon(true);
        probeThread.start();
        // Wait for the probe thread to finish.
        while(probeThread.getState() != Thread.State.TERMINATED) {}
        // Return all of the available devices in the form of ReachableQuDevice objects.
        return availableDevices;
    }
    
    public ReachableQuDevice createReachableQuDeviceObject(String serverAddress) {
    	Socket probeSocket = null;
    	ReachableQuDevice locatedDevice = null;
        try {
            // Create a socket on which to probe for a response from the Qu Server.
            probeSocket = new Socket();
            try {
                probeSocket.connect(new InetSocketAddress(serverAddress, C.PROBE_CLIENT_RECEIVER_PORT), C.PROBE_SOCKET_CONNECT_TIMEOUT);
            } catch (IOException e) {}

            // Set socket timeout as the target will in most cases not respond.
            probeSocket.setSoTimeout(C.PROBE_SOCKET_READ_TIMEOUT);

            BufferedReader br = new BufferedReader(new InputStreamReader(probeSocket.getInputStream()));
            String response = br.readLine();

            // Construct a JSON object using the devices response.
            JSONObject drJSON = new JSONObject(response);

            // Construct a String array of super client id's.
            String[] superClientIds = new String[drJSON.getJSONArray("super_users").length()];
            for(int i = 0; i < superClientIds.length; i++) {
                superClientIds[i] = drJSON.getJSONArray("super_users").getString(i);
            }

            // Create our representation of the reachable device.
            locatedDevice = new ReachableQuDevice(drJSON.getString("device_id"),
                    drJSON.getString("device_name"),
                    drJSON.getInt("afr_port"),
                    drJSON.getInt("cm_port"),
                    serverAddress,
                    drJSON.getBoolean("isProtected"),
                    superClientIds);
     
        } catch (IOException e)
        {} catch (JSONException jE) {}
        finally {
            // Close the socket
            try {
                if(probeSocket != null) {
                    probeSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Return the ReachableQuDevice instance we made.
    	return locatedDevice;
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
}
