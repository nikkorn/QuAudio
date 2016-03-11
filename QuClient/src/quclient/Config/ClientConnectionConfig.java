package quclient.Config;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class ClientConnectionConfig {
    private String clientId;
    private String clientName;
    private String accessPassword = "";
    private boolean locked = false;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        // Has this ClientConfig object been locked?
        if(locked) {
            throw new RuntimeException("this ClientConfig object is locked");
        }
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        // Has this ClientConfig object been locked?
        if(locked) {
            throw new RuntimeException("this ClientConfig object is locked");
        }
        this.clientName = clientName;
    }

    public String getAccessPassword() {
        return accessPassword;
    }

    public void setAccessPassword(String accessPassword) {
        // Has this ClientConfig object been locked?
        if(locked) {
            throw new RuntimeException("this ClientConfig object is locked");
        }
        this.accessPassword = accessPassword;
    }

    /**
     * Sets a flag that stops the user from being able to alter any variables of this object.
     * Called when initialising a device object as the information this class contains cannot
     * be changed once we establish a connection with a Qu Server instance.
     */
    public void lock() {
        locked = true;
    }
}
