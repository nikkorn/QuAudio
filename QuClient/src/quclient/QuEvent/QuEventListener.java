package quclient.QuEvent;

import quclient.NetProbe.ReachableQuDevice;
import quclient.Server.Device;
import quclient.Server.HandshakeResponse;

/**
 * An observer for listening for Device state changes.
 * @author Nikolas Howard 
 *
 */
public interface QuEventListener {

    /**
     * Called when the Device settings are updated.
     * @param sourceDevice
     */
    public void onQuSettingsUpdate(Device sourceDevice);

    /**
     * Called when the Device PlayList is updated.
     * @param sourceDevice
     */
    public void onQuPlayListUpdate(Device sourceDevice);

    /**
     * Called when the Device master volume is updated.
     * @param sourceDevice
     */
    public void onQuMasterVolumeUpdate(Device sourceDevice);

    /**
     * Called when the connection to the QuServer is broken.
     * @param sourceDevice
     */
    public void onQuDisconnect(Device sourceDevice);

    /**
     * Called when the connection to the QuServer is made.
     * @param sourceDevice
     */
    public void onQuConnect(Device sourceDevice);

    /**
     * Called when we outright fail to connect to a server for any reason.
     * @param The ReachableQuDevice instance that was used in the attempt to connect.
     * @param handshakeResponse
     */
    public void onQuLinkFailure(ReachableQuDevice device, HandshakeResponse handshakeResponse);
}
