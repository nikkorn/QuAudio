package NetBeacon;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import Server.Log;
import Server.Server;

/**
 * Listens for probe requests from client NetProbe. 
 * @author Nikolas Howard
 *
 */
public class Beacon implements Runnable {
	ServerSocket beaconServerSocket = null;
	
	public Beacon(int port) {
		try {
			beaconServerSocket = new ServerSocket(port);
		} catch (IOException e) {
			// If this fails they quit the app.
			Log.log(Log.MessageType.CRITICAL, "NETPROBE_BEACON", "failed to initialise beacon server socket");
			Log.forceWrite();
			System.exit(0);
		}
	}

	@Override
	public void run() {
		// Continuously listen for probes from clients.
		while(true) {
			boolean gotSocket = true;
			Socket senderSocket = null;
			try {
				senderSocket = beaconServerSocket.accept();
			} catch (IOException e) {
				// If we get an IOException here then just log it as an unsuccesful connection.
				gotSocket = false;
				Log.log(Log.MessageType.WARNING, "NETPROBE_BEACON", "failed to get connecting client socket");
			}
			// Only continue if we successfully grabbed a Socket.
			if(gotSocket) {
				Log.log(Log.MessageType.INFO, "NETPROBE_BEACON", "got probe from client at '" + senderSocket.getRemoteSocketAddress().toString() + "'");
				// Get all property info we need to send as a response to the probe.
				String deviceId = Server.properties.getDeviceId();
				String deviceName = Server.properties.getDeviceName();
				int afrPort = Server.properties.getAudioFileReceiverPort();
				int cmPort = Server.properties.getClientManagerPort();
				ArrayList<String> superUserClientIds = Server.properties.getSuperUsers();
				boolean isProtected = !Server.properties.getAccessPassword().equals("");
				
				// Now that we have all of the information we need to send back to the client we can begin to construct our JSON response.
				JSONObject jO = new JSONObject();
				jO.put("device_id", deviceId);
				jO.put("device_name", deviceName);
				jO.put("afr_port", afrPort);
				jO.put("cm_port", cmPort);
				jO.put("isProtected", isProtected);
				
				// Create an independent JSON object to hold our super user info.
				JSONArray superUserJSONArray = new JSONArray();
				for(String suClientId : superUserClientIds) {
					superUserJSONArray.put(suClientId);
				}
				jO.put("super_users", superUserJSONArray);
				
				// Create a PrintWriter with which to write our JSON via our client sockets output stream.
				PrintWriter pw = null;
				boolean printWriterInitialisationFailed = false;
				try {
					pw = new PrintWriter(senderSocket.getOutputStream());
				} catch (IOException e) {
					printWriterInitialisationFailed = true;
					Log.log(Log.MessageType.WARNING, "NETPROBE_BEACON", "failed to initialise PUSH_PROBE_RESPONSE PrintWriter");
				}
				
				try {
					// Print our JSON response to the socket only if there was no error in setting up out printwriter.
					if(!printWriterInitialisationFailed) {
						pw.println(jO);
						pw.flush();
					}
				} catch (Exception e) {
					Log.log(Log.MessageType.WARNING, "NETPROBE_BEACON", "encountered an error when sending PUSH_PROBE_RESPONSE JSON to client");
				} finally {
					if(pw != null) {
						pw.close();
					}
				}
			}
		}
	}
}