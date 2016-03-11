package quclient.Server;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Reperesents an action to be sent to the Qu Server
 * @author Nikolas Howard
 *
 */
public class OutgoingAction extends Action {

    public OutgoingAction(OutgoingActionType type, JSONObject infoObject) {
        // Set the action type in the JSON object.
        try {
            infoObject.put("action_type", type.toString());
        } catch (JSONException j) {}
        this.actionInfoObject = infoObject;
    }

    /**
     * Get the OutgoingActionType of this Action.
     * @return OutgoingActionType
     */
    public OutgoingActionType getOutgoingActionType() {
        OutgoingActionType actionType = null;
        try {
            actionType =  OutgoingActionType.valueOf(this.getActionInfoObject().getString("action_type"));
        } catch (JSONException j) {}
        return actionType;
    }
}
