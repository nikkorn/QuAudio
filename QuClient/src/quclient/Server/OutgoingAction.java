package quclient.Server;

import org.json.JSONObject;

/**
 * Reperesents an action to be sent to the Qu Server
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
