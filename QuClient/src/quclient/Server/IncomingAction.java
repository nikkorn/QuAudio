package quclient.Server;

import org.json.JSONObject;

/**
 * Represents an action sent by the server
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
		return IncomingActionType.valueOf(this.getActionInfoObject().getString("action_type"));
	}
}
