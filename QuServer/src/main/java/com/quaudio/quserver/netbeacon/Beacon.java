package com.quaudio.quserver.netbeacon;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import com.quaudio.quserver.server.Log;
import com.quaudio.quserver.server.Server;

/**
 * Listens for probe requests from client NetProbe. 
 * @author Nikolas Howard
 *
 */
public class Beacon {
	// Used to accept connections from clients that have found this server on the network and return various details.
	private ServerSocket clientReceiverSocket = null;
	// Listens for UDP packets (probes) sent from clients, used to return a confirmation of 
	// of this servers presence on the local network.
	private DatagramSocket probeListenerSocket = null;
	
	public Beacon(int beaconPort, int receiverPort) {
		try {
			clientReceiverSocket = new ServerSocket(receiverPort);
		} catch (IOException e) {
			// If this fails they quit the app.
			Log.log(Log.MessageType.CRITICAL, "NETPROBE_BEACON", "failed to initialise beacon receiver");
			Log.forceWrite();
			System.exit(0);
		}
		try {
			probeListenerSocket = new DatagramSocket(beaconPort, InetAddress.getByName("0.0.0.0"));
			probeListenerSocket.setBroadcast(true);
		} catch (IOException e) {
			// If this fails they quit the app.
			Log.log(Log.MessageType.CRITICAL, "NETPROBE_BEACON", "failed to initialise beacon probe listener");
			Log.forceWrite();
			System.exit(0);
		}
	}
	
	/**
	 * Start the beacon.
	 */
	public void start() {
		// Start the probe listener and client receiver in separate threads.
		Thread probeListenerThread = new Thread(new Runnable() {
			@Override
			public void run() { runProbeListener(); }
		});
		probeListenerThread.setDaemon(true);
		probeListenerThread.start();
		Thread clientReceiverThread = new Thread(new Runnable() {
			@Override
			public void run() { runClientReceiver(); }
		});
		clientReceiverThread.setDaemon(true);
		clientReceiverThread.start();
	}
	
	/**
	 * Stop the beacon by closing the clientReceiverSocket and probeListenerSocket.
	 */
	public void stop() {
		// Close the clientReceiverSocket and probeListenerSocket.
		if(clientReceiverSocket != null) {
			try {
				clientReceiverSocket.close();
			} catch (IOException e) {}
		}
		if(probeListenerSocket != null) {
			probeListenerSocket.close();
		}
	}
	
	/**
	 * Listens for UDP packets (probes) in its own thread and on finding one returns a confirmation packet.
	 * This allows clients to locate servers on the network.
	 */
	public void runProbeListener() {
		// Continuously listen for probes.
		while(true) {
			byte[] packetBuffer = new byte[15000];
			DatagramPacket potentialProbe = new DatagramPacket(packetBuffer, packetBuffer.length);
			try {
				probeListenerSocket.receive(potentialProbe);
				// We received a packet, check its contents to see if it is a probe.
				String packetMessage = new String(potentialProbe.getData());
				if(packetMessage.trim().equals("QU_C_PRB")) {
					// This is a probe!
					Log.log(Log.MessageType.INFO, "NETPROBE_BEACON", "got probe from '" + potentialProbe.getAddress().getHostAddress() + "', returning response");
					// Form our response message.
					byte[] sendData = "QU_S_RSP".getBytes();
					// Send our response.
					DatagramPacket probeResponse = new DatagramPacket(sendData, sendData.length, potentialProbe.getAddress(), potentialProbe.getPort());
					probeListenerSocket.send(probeResponse);
				}
			} catch (IOException e) {
				// If this IOException is thrown by the probeListenerSocket being closed 
				// then simply stop the running probe listener thread.
				if(probeListenerSocket == null || probeListenerSocket.isClosed()) {
					break;
				}
			}
		}
	}
	
	/**
	 * Waits for connections from clients that have previously found the server via a probe.
	 * On getting a connection, we return various server details.
	 */
	public void runClientReceiver() {
		// Continuously listen for connections from clients.
		while(true) {
			boolean gotSocket = true;
			Socket senderSocket = null;
			try {
				senderSocket = clientReceiverSocket.accept();
			} catch (IOException e) {
				// If this IOException is thrown by the clientReceiverSocket being closed 
				// then simply stop the running client receiver thread.
				if(clientReceiverSocket == null || clientReceiverSocket.isClosed()) {
					break;
				}
				// Our clientReceiverSocket is fine, just log this as an unsuccessful connection.
				gotSocket = false;
				Log.log(Log.MessageType.WARNING, "NETPROBE_BEACON", "failed to get connecting client socket");
			}
			// Only continue if we successfully grabbed a Socket.
			if(gotSocket) {
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