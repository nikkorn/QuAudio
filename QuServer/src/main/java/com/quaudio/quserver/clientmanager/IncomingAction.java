package com.quaudio.quserver.clientmanager;

import org.json.JSONObject;
import com.quaudio.quserver.clientmanager.Action;
import com.quaudio.quserver.clientmanager.IncomingActionType;

/**
 * Represents an action sent by the client
 * @author Nikolas Howard
 *
 */
public class IncomingAction extends Action {
	
	public IncomingAction(JSONObject infoObject) {
		this.actionInfoObject = infoObject;
	}
	
	/**
	 * Get the IncomingActionType of this Action.
	 * @return IncomingActionType
	 */
	public IncomingActionType getIncomingActionType() {
		try {
			return IncomingActionType.valueOf(this.getActionInfoObject().getString("action_type"));
		} catch (IllegalArgumentException e) {
			// We have no knowledge of this action type, return 'UNKNOWN'
			return IncomingActionType.UNKNOWN;
		}
	}
}
