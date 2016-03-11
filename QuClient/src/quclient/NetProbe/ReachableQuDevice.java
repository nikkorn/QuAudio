package quclient.NetProbe;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class ReachableQuDevice {
    private String deviceId;
    private String deviceName;
    private String address;
    private int afrPort;
    private int cmPort;
    private boolean isProtected;
    private String[] superUsers;

    public ReachableQuDevice(String deviceId, String deviceName, int afrPort, int cmPort, String address, boolean isProtected, String[] superUsers) {
        this.deviceId = deviceId;
        this.address = address;
        this.isProtected = isProtected;
        this.deviceName = deviceName;
        this.afrPort = afrPort;
        this.cmPort = cmPort;
        this.superUsers = superUsers;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getAddress() {
        return address;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getAudioFileReceiverPort() {
        return afrPort;
    }

    public int getClientManagerPort() {
        return cmPort;
    }

    public boolean isSuperUser(String clientId) {
        for(String superClientId : superUsers) {
            if(superClientId.equals(clientId)) {
                return true;
            }
        }
        return false;
    }

    public String[] getSuperUserIds() {
        return superUsers;
    }
}
