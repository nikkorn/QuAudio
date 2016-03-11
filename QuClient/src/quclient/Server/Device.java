package quclient.Server;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import quclient.Config.ClientConnectionConfig;
import quclient.Config.PasswordUtils;
import quclient.FileTransfer.AudioFileSender;
import quclient.FileTransfer.FileFormat;
import quclient.NetProbe.ReachableQuDevice;
import quclient.ProxyPlaylist.PlayList;
import quclient.ProxyPlaylist.Track;
import quclient.ProxyPlaylist.TrackState;
import quclient.QuEvent.QuEventListener;
import quclient.QuEvent.QuEventType;

/**
 * Represents an instance of a Qu Server
 * @author Nikolas Howard
 *
 */
public class Device {
    private ReachableQuDevice reachableDevice;
    private ClientConnectionConfig clientConfig;
    private ActionChannel actionChannel;

    // Has this instance been fully initialised?
    private volatile boolean initilised = false;

    // Variables that relate to waiting for our welcome package?
    private Object welcomePackageCheckLock = new Object();
    private final int DEVICE_LINK_WELCOMEPACKAGE_TIMEOUT = 4000;
    private volatile boolean receivedPlayListUpdate = false;
    private volatile boolean receivedVolumeUpdate = false;

    // Variables that are initially set by the input ReachableQuDevice, but can change during the lifetime of a Device object.
    private Object settingsUpdateLock = new Object();
    private volatile boolean areSettingsDirty = false;
    private String deviceName;
    private boolean isProtected;
    private String[] superUsers;
    private int volumeLevel = 0;

    // List of subscribing QuEventListeners who will be updated on events created by the IncomingAction processing thread.
    private ArrayList<QuEventListener> subscribingQuEventListeners = new ArrayList<QuEventListener>();

    // Represents the latest state of our Playlist
    private JSONObject proxyPlaylist = null;
    // Reference to the last instance of PlayList that was constructed, this is needed as we will have to set
    // the object as dirty the next time that the client receives a PUSH_PLAYLIST IncomingAction from the server
    private PlayList lastPlayList = null;

    /**
     * Fully initialises this instance of Device by actually attempting to connect to the QuServer, populating
     * vital information with the use of a welcome package (the first send of vital system info IncomingActions)
     * and then marking the instance as fully initialised (which is required in order for the user to be able to
     * invoke pretty much every method of this class except those relating to QuEventListener subscriptions).
     * @param reachableDevice
     * @param clientConfig
     * @throws IOException
     * @throws RuntimeException
     */
    public void link(ReachableQuDevice reachableDevice, ClientConnectionConfig clientConfig) throws IOException, RuntimeException {
        this.reachableDevice = reachableDevice;
        this.deviceName = reachableDevice.getDeviceName();
        this.isProtected = reachableDevice.isProtected();
        this.superUsers = reachableDevice.getSuperUserIds();
        // Lock the ClientConfig object so that its state cannot be altered from here on out.
        clientConfig.lock();
        this.clientConfig = clientConfig;
        // Create an ActionChannel.
        this.actionChannel = new ActionChannel(reachableDevice.getAddress(), reachableDevice.getClientManagerPort(),
                clientConfig.getClientId(), clientConfig.getClientName(), clientConfig.getAccessPassword());

        // Start a new thread, this will be responsible for processing IncomingActions.
        Thread deviceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean connected = true;
                // Only process actions if we are still connected, otherwise don't bother.
                while(connected) {
                    // Wait a bit so we don't hog too much processing power.
                    try {
                        Thread.sleep(6);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Process incoming actions and check that we were still connected.
                    connected = processIncomingActions();
                }
                // We have disconnected, notify subscribing QuEventListeners.
                notifyQuEventListeners(QuEventType.DISCONNECTION);
            }
        });
        deviceThread.setDaemon(true);
        deviceThread.start();

        // Don't return until we get our welcome package or we timeout.
        long welcomePackageWaitStart = System.currentTimeMillis();
        while(true) {
            // Wait for a bit, no need to blitz this thread.
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Have we exceeded the timeout for waiting for a welcome package?
            if((System.currentTimeMillis() - welcomePackageWaitStart) > this.DEVICE_LINK_WELCOMEPACKAGE_TIMEOUT) {
                throw new RuntimeException("exceeded timeout waiting for welcome package from server");
            }
            // Have we received our welcome package yet?
            synchronized(welcomePackageCheckLock) {
                // Check that we have all the initial updates we need in order for the user to start interacting with this object.
                if(receivedPlayListUpdate && receivedVolumeUpdate) {
                    break;
                }
            }
        }

        // We can now regard this object as being fully initialised.
        this.initilised = true;
    }

    /**
     * Get the device id of the QuServer.
     * @return id
     */
    public String getDeviceId() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        return reachableDevice.getDeviceId();
    }

    /**
     * Get the network address of the QuServer.
     * @return address
     */
    public String getAddress() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        return reachableDevice.getAddress();
    }

    /**
     * Get the port on which the QuServer is listening for incoming Audio File uploads.
     * @return listening port
     */
    public int getAudioFileReceiverPort() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        return reachableDevice.getAudioFileReceiverPort();
    }

    /**
     * Get the port on which the QuServer is listening for incoming Client connection requests.
     * @return listening port
     */
    public int getClientManagerPort() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        return reachableDevice.getClientManagerPort();
    }

    /**
     * Returns true if the QuServer requires an access password in order to connect.
     * This value can be updated by the server during the lifetime of a Device instance.
     * @return isProtected
     */
    public boolean isProtected() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        synchronized(settingsUpdateLock) {
            return this.isProtected;
        }
    }

    /**
     * Returns the name of the QuServer.
     * This value can be updated by the server during the lifetime of a Device instance.
     * @return quserver name
     */
    public String getDeviceName() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        synchronized(settingsUpdateLock) {
            return this.deviceName;
        }
    }

    /**
     * Returns the master volume of the device hosting the QuServer.
     * This value can be updated by the server during the lifetime of a Device instance.
     * @return volume level (range 0 - 100)
     */
    public int getDeviceVolume() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        synchronized(settingsUpdateLock) {
            return this.volumeLevel;
        }
    }

    /**
     * Get the client configuration.
     * @return client configuration
     */
    public ClientConnectionConfig getClientConfiguration() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        return this.clientConfig;
    }

    /**
     * Returns true if the client that initalised the Device object instance is an admin at the time.
     * This value can be updated by the server during the lifetime of a Device instance.
     * @return is admin (super) user.
     */
    public boolean adminModeEnabled() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        synchronized(settingsUpdateLock) {
            for(String superClientId : superUsers) {
                if(superClientId.equals(clientConfig.getClientId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the most up to date version of our PlayList since process() last handled a PUSH_PLAYLIST IncomingAction
     * @return latest playlist
     */
    public PlayList getPlayList() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        // Construct a new ordered list to store tracks
        LinkedList<Track> constructedPlayListTracks = new LinkedList<Track>();
        // Populate our list of tracks
        synchronized(proxyPlaylist) {
            try {
                JSONArray orderedTrackJSONArray = proxyPlaylist.getJSONArray("playlist");
                for (int trackIndex = 0; trackIndex < orderedTrackJSONArray.length(); trackIndex++) {
                    JSONObject trackJSON = orderedTrackJSONArray.getJSONObject(trackIndex);
                    Track newTrack = new Track(this);
                    newTrack.setTrackId(trackJSON.getString("track_id"));
                    newTrack.setOwnerId(trackJSON.getString("owner_id"));
                    newTrack.setTrackState(TrackState.valueOf(trackJSON.getString("track_state")));
                    newTrack.setName(trackJSON.getString("name"));
                    newTrack.setArtist(trackJSON.getString("artist"));
                    newTrack.setAlbum(trackJSON.getString("album"));
                    // Add the new Track to the list
                    constructedPlayListTracks.add(newTrack);
                }
            } catch (JSONException j) {}
        }
        PlayList constructedPlayList = new PlayList(constructedPlayListTracks);
        // Keep a reference to this object so we can mark it as dirty when we get an update on the PlayList
        this.lastPlayList = constructedPlayList;
        return constructedPlayList;
    }

    /**
     * Uploads an audio file to the Qu Server.
     * @param audioFile
     * @param format
     * @param name
     * @param artist
     * @param album
     */
    public void uploadAudioFile(File audioFile, FileFormat format, String name, String artist, String album) {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        AudioFileSender audioFileSender = new AudioFileSender(reachableDevice.getAddress(), reachableDevice.getAudioFileReceiverPort());
        try {
            audioFileSender.upload(audioFile, clientConfig.getClientId(), format.toString(), name, artist, album);
        } catch (UnknownHostException e) {
            // Upload failed, do nothing as this shouldn't effect the user,
            e.printStackTrace();
        } catch (IOException e) {
            // Upload failed, do nothing as this shouldn't effect the user,
            e.printStackTrace();
        }
    }

    /**
     * Send an outgoing action to the server
     * @param outgoingAction
     */
    public void sendAction(OutgoingAction outgoingAction) {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        // TODO check to see if this user has permissions for the action they want to send
        if(actionChannel.isConnected()) {
            actionChannel.sendOutgoingActionToServer(outgoingAction);
        } else {
            // We are no longer connected to the server
            throw new RuntimeException("not connected to Qu server");
        }
    }

    /**
     * Sends an update of QuServer settings to the server.
     * @param serverName
     * @param accessPassword
     */
    public void updateQuServerSettings(String serverName, String accessPassword, String superPassword) {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        // Make sure the client is a super user.
        if(!adminModeEnabled()) {
            throw new RuntimeException("must be a super user to update server settings");
        }
        JSONObject updateSettingsJSON = new JSONObject();
        try {
            updateSettingsJSON.put("device_name", serverName);
            // Include the client id so we can double-check that this user is super server-side.
            updateSettingsJSON.put("client_id", this.clientConfig.getClientId());
            // If accessPassword is null then we are assuming the user does not want to change it.
            if (accessPassword == null) {
                updateSettingsJSON.put("access_password", "?"); // Server will pick up '?' as meaning 'don't update'
            } else {
                // The user wants to update the access password, make sure that it is a valid password. or an empty string will clear the access password.
                if (PasswordUtils.isValidPassword(accessPassword) || accessPassword.equals("")) {
                    updateSettingsJSON.put("access_password", accessPassword);
                } else {
                    throw new RuntimeException("'" + accessPassword + "' is not a valid access password, must consist of four characters, either digits or letters");
                }
            }
            // If superPassword is null then we are assuming the user does not want to change it.
            if (superPassword == null) {
                updateSettingsJSON.put("super_password", "?"); // Server will pick up '?' as meaning 'don't update'
            } else {
                // The user wants to update the super password, make sure that it is a valid password.
                if (PasswordUtils.isValidPassword(superPassword)) {
                    updateSettingsJSON.put("super_password", superPassword);
                } else {
                    throw new RuntimeException("'" + superPassword + "' is not a valid super password, must consist of four characters, either digits or letters");
                }
            }
            OutgoingAction settingsUpdateAction = new OutgoingAction(OutgoingActionType.UPDATE_SETTINGS, updateSettingsJSON);
            sendAction(settingsUpdateAction);
        } catch (JSONException j) {}
    }

    /**
     * Process each IncomingAction
     * @return Whether the connection was broken
     */
    private boolean processIncomingActions() {
        if(actionChannel.isConnected()) {
            // Catch any settings updates and apply them before handing the IncomingAction to the user.
            IncomingAction incomingAction = actionChannel.getIncomingActionFromList();
            // Process all actions
            while(incomingAction != null) {
                // Process each differently based on their type
                switch(incomingAction.getIncomingActionType()) {
                case PLAY_FAIL:
                    break;

                case PUSH_PLAYLIST:
                    applyPlayListUpdate(incomingAction);
                    // Notify subscribing QuEventListeners.
                    notifyQuEventListeners(QuEventType.PLAYLIST_UPDATED);
                    break;

                case PUSH_VOLUME:
                    applyVolumeUpdate(incomingAction);
                    // Notify subscribing QuEventListeners.
                    notifyQuEventListeners(QuEventType.VOLUME_UPDATED);
                    break;

                case PUSH_SETTINGS:
                    applySettingsUpdate(incomingAction);
                    // Notify subscribing QuEventListeners.
                    notifyQuEventListeners(QuEventType.SETTINGS_UPDATED);
                    break;
                }

                // Fetch next incoming action
                incomingAction = actionChannel.getIncomingActionFromList();
            }
        } else {
            // We are disconnected, return that this failed
            return false;
        }
        // We are still connected, return true
        return true;
    }

    /**
     * Applies a PlayList state change to this Device.
     * @param incomingAction
     */
    private void applyPlayListUpdate(IncomingAction playlistUpdateAction) {
        // Store the JSON representation of our PlayList for the next time the user wants it.
        if(proxyPlaylist == null) {
            this.proxyPlaylist = playlistUpdateAction.getActionInfoObject();
        } else {
            synchronized(proxyPlaylist) {
                this.proxyPlaylist = playlistUpdateAction.getActionInfoObject();
            }
        }
        // We have a new snapshot of the state of the PlayList, if the user has grabbed a PlayList before
        // we need to mark it as dirty to let them know it is out-dated.
        if(this.lastPlayList != null) {
            lastPlayList.setDirty(true);
        }
        // This update may be part of our welcome package, set receivedPlayListUpdate just in case it is
        synchronized(welcomePackageCheckLock) {
            receivedPlayListUpdate = true;
        }
    }

    /**
     * Applies a system volume change to this Device.
     * @param incomingAction
     */
    private void applyVolumeUpdate(IncomingAction volumeUpdateAction) {
        synchronized(settingsUpdateLock) {
            try {
                this.volumeLevel = volumeUpdateAction.getActionInfoObject().getInt("volume_level");
            } catch (JSONException j) {}
            this.areSettingsDirty = true;
        }
        // This update may be part of our welcome package, set receivedVolumeUpdate just in case it is
        synchronized(welcomePackageCheckLock) {
            receivedVolumeUpdate = true;
        }
    }

    /**
     * Applies settings changes that were passed from the QuServer.
     * @param settingsUpdateAction
     */
    private void applySettingsUpdate(IncomingAction settingsUpdateAction) {
        // Construct a String array of super client ids
        try {
            String[] superClientIds = new String[settingsUpdateAction.getActionInfoObject().getJSONArray("super_users").length()];
            for (int i = 0; i < superClientIds.length; i++) {
                superClientIds[i] = settingsUpdateAction.getActionInfoObject().getJSONArray("super_users").getString(i);
            }
            synchronized (settingsUpdateLock) {
                this.deviceName = settingsUpdateAction.getActionInfoObject().getString("device_name");
                this.isProtected = settingsUpdateAction.getActionInfoObject().getBoolean("isProtected");
                this.superUsers = superClientIds;
                this.areSettingsDirty = true;
            }
        } catch (JSONException j) {}
    }

    /**
     * Returns true if the settings of the device (e.g. name, volume) have changed since the last time hasDirtySettings() was called.
     * @return hasDirtySettings
     */
    public boolean hasDirtySettings() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        boolean dirty = false;
        synchronized(settingsUpdateLock) {
            dirty = this.areSettingsDirty;
            this.areSettingsDirty = false;
        }
        return dirty;
    }

    /**
     * Adds a new QuEventListener to our list of listeners to be notified on device state changes.
     * @param eventListener
     */
    public void addQuEventListener(QuEventListener newEventListener) {
        synchronized(subscribingQuEventListeners) {
            // Is this already a subscriber? If so then just return.
            if(subscribingQuEventListeners.contains(newEventListener)) {
                return;
            }
            // Add the new subscriber.
            subscribingQuEventListeners.add(newEventListener);
        }
    }

    /**
     * Adds a new QuEventListener to our list of listeners to be notified on device state changes.
     * @param eventListener
     */
    public void removeQuEventListener(QuEventListener eventListener) {
        synchronized(subscribingQuEventListeners) {
            // Only remove if this is already a subscriber
            if(subscribingQuEventListeners.contains(eventListener)) {
                subscribingQuEventListeners.remove(eventListener);
            }
        }
    }

    /**
     * Notify any subscribing QuEventListeners of a QuEvent that was picked up when processing IncomingActions.
     * @param The type of event
     */
    private void notifyQuEventListeners(QuEventType eventType) {
        // We will not raise any QuEvents until this object is fully initialised as users will most likely attempt
        // to interact with the Device instance on receiving the event, which wont be great if were not fully initialised.
        if(!this.initilised) {
            return;
        }
        // We need a reference to this Device instance.
        Device sourceDevice = this;
        synchronized(subscribingQuEventListeners) {
            // Notify each subscriber in turn.
            for(QuEventListener subscriber : subscribingQuEventListeners) {
                // The type of handling method we call depends on the event type.
                switch(eventType) {
                case DISCONNECTION:
                    // Call the appropriate event handler.
                    subscriber.quQuDisconnect(sourceDevice);
                    break;
                case PLAYLIST_UPDATED:
                    // Call the appropriate event handler.
                    subscriber.onQuPlayListUpdate(sourceDevice);
                    break;
                case SETTINGS_UPDATED:
                    // Call the appropriate event handler.
                    subscriber.onQuSettingsUpdate(sourceDevice);
                    break;
                case VOLUME_UPDATED:
                    // Call the appropriate event handler.
                    subscriber.onQuMasterVolumeUpdate(sourceDevice);
                    break;
                }
            }
        }
    }

    /**
     * Returns true/false depending on if we are connected to the server.
     * @return isConnected
     */
    public boolean isConnected() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        return this.actionChannel.isConnected();
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        // The user must have fully initialised this object.
        if(!initilised) {
            throw new RuntimeException("Device is not fully initialised, call link()");
        }
        // Disconnect the ActionChannel from the server.
        this.actionChannel.disconnect();
    }
}
