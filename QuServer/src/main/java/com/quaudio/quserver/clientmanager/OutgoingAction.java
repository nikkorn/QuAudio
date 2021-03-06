package com.quaudio.quserver.clientmanager;

import org.json.JSONObject;
import com.quaudio.quserver.clientmanager.Action;
import com.quaudio.quserver.clientmanager.OutgoingActionType;

/**
 * Reperesents an action to be sent to a client
 * @author Nikolas Howard
 *
 */
public class OutgoingAction extends Action {
	
	public OutgoingAction(OutgoingActionType type, JSONObject infoObject) {
		// Set the action type in the JSON object.
		infoObject.put("action_type", type.toString());
		this.actionInfoObject = infoObject;
	}
	
	/**
	 * Get the OutgoingActionType of this Action.
	 * @return OutgoingActionType
	 */
	public OutgoingActionType getOutgoingActionType() {
		return OutgoingActionType.valueOf(this.getActionInfoObject().getString("action_type"));
	}
}
