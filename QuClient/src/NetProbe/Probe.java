package NetProbe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.json.JSONObject;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class Probe implements Runnable{
    private String address;
    private NetProbe netProbe;

    public Probe(NetProbe probe, String address){
        this.address = address;
        this.netProbe = probe;
    }

    @Override
    public void run() {
        try {
            // Create a socket on which to probe for a response from the Qu Server
            Socket probeSocket = new Socket();
            probeSocket.connect(new InetSocketAddress(address, C.PROBE_PORT), C.PROBE_SOCKET_CONNECT_TIMEOUT);

            // Set socket timeout as the target will in most cases not respond
            probeSocket.setSoTimeout(C.PROBE_SOCKET_READ_TIMEOUT);

            BufferedReader br = new BufferedReader(new InputStreamReader(probeSocket.getInputStream()));
            String response = br.readLine();
            
            // Construct a JSON object using the devices response.
            // TODO Catch cases where whatever is returned is NOT our exprected response
            JSONObject drJSON = new JSONObject(response);
			
            // Create our representation of the reachable device. 
            ReachableQuDevice locatedDevice = new ReachableQuDevice(drJSON.getString("device_id"), 
            		drJSON.getString("device_name"), 
            		drJSON.getInt("afr_port"), 
            		drJSON.getInt("cm_port"), 
            		address, 
            		drJSON.getBoolean("isProtected"));
            
            // Add the new device to the list
            netProbe.addReachableDevice(locatedDevice);

            // Close the socket
            probeSocket.close();
        } catch (IOException e) {}
    }
}
