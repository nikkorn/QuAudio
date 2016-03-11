package quclient.NetProbe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.json.JSONException;
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
            try {
                probeSocket.connect(new InetSocketAddress(address, C.PROBE_PORT), C.PROBE_SOCKET_CONNECT_TIMEOUT);
            } catch (IOException e) {}

            // Set socket timeout as the target will in most cases not respond
            probeSocket.setSoTimeout(C.PROBE_SOCKET_READ_TIMEOUT);

            BufferedReader br = new BufferedReader(new InputStreamReader(probeSocket.getInputStream()));
            String response = br.readLine();
            
            // Construct a JSON object using the devices response.
            JSONObject drJSON = new JSONObject(response);
            
            // Construct a String array of super client ids
            String[] superClientIds = new String[drJSON.getJSONArray("super_users").length()];
            for(int i = 0; i < superClientIds.length; i++) {
                superClientIds[i] = drJSON.getJSONArray("super_users").getString(i);
            }

            // Create our representation of the reachable device.
            ReachableQuDevice locatedDevice = new ReachableQuDevice(drJSON.getString("device_id"), 
                    drJSON.getString("device_name"),
                    drJSON.getInt("afr_port"),
                    drJSON.getInt("cm_port"),
                    address,
                    drJSON.getBoolean("isProtected"),
                    superClientIds);

            
            // Add the new device to the list
            netProbe.addReachableDevice(locatedDevice);

            // Close the socket
            probeSocket.close();
        } catch (IOException e)
        {} catch (JSONException jE) {}
    }
}
