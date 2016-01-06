package ClientManager;

import org.json.JSONObject;
import ClientManager.Action;
import ClientManager.IncomingActionType;

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
		return IncomingActionType.valueOf(this.getActionInfoObject().getString("action_type"));
	}
}
