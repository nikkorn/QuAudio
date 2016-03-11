package quclient.FileTransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class AudioFileSender {
    private int afrPort;
    private String targetAddress;

    public AudioFileSender(String afrAddress, int afrPort) {
        this.targetAddress = afrAddress;
        this.afrPort = afrPort;
    }

    /**
     * Uploads an audio file to the Qu Server's AudioFileReceiver
     * @param audioFile
     * @param clientId
     * @param format
     * @param name
     * @param artist
     * @param album
     * @throws IOException
     * @throws UnknownHostException
     */
    public void upload(final File audioFile, final String clientId, final String format, final String name, final String artist, final String album) throws UnknownHostException, IOException {
        // Start the upload on a new thread
        new Thread(new Runnable() {

            @Override
            public void run() {
                // Create a new socket with which to connect to the server
                Socket afrSocket = null;
                try {
                    afrSocket = new Socket(targetAddress, afrPort);
                } catch (UnknownHostException e) {
                    // Failed to get connection to server AudioFileReceiver, bow out
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    // Failed to get connection to server AudioFileReceiver, bow out
                    e.printStackTrace();
                    return;
                }
                // Attempt to send the JSON defining the audio track to the server
                boolean infoJSONSent = sendInfoJSON(afrSocket, clientId, format, name, artist, album);
                if(infoJSONSent) {
                    // Send the physical file
                    sendPhysicalFile(afrSocket, audioFile);
                }
            }

        }).start();
    }

    /**
     * Sends a JSON object defining an audio track to the server to be picked up by the AudioFileReceiver.
     * @param socket
     * @param clientId
     * @param format
     * @param name
     * @param artist
     * @param album
     * @return Returns true/false depending on whether sending the JSON string was a success
     */
    private boolean sendInfoJSON(Socket socket, String clientId, String format, String name, String artist, String album) {
        PrintWriter pw = null;
        try {
            JSONObject jO = new JSONObject();
            jO.put("client_id", clientId);
            jO.put("name", name);
            jO.put("artist", artist);
            jO.put("album", album);
            jO.put("format", format);

            try {
                pw = new PrintWriter(socket.getOutputStream());
            } catch (FileNotFoundException e) {
                // We failed to write our JSON object to the server, return false
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                // We failed to write our JSON object to the server, return false
                e.printStackTrace();
                return false;
            }

            // Send our JSON
            pw.println(jO);
            pw.flush();
        } catch (JSONException j) {
            // TODO handle error
            return false;
        }

        // We had no issue, return true
        return true;
    }

    /**
     * Sends the physical audio file to the server to be picked up by the AudioFileReceiver
     * @param socket
     * @param source
     */
    private void sendPhysicalFile(Socket socket, File source) {
        FileInputStream fis = null;
        OutputStream output = null;

        try {
            fis = new FileInputStream(source);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            output = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            int count;
            byte[] bytes = new byte[1024];
            while ((count = fis.read(bytes)) > 0) {
                output.write(bytes, 0, count);
            }
            output.flush();
            output.close();
        } catch (IOException e) {
            // We failed to write our audio file to the server, do nothing.
            e.printStackTrace();
        }
    }
}
