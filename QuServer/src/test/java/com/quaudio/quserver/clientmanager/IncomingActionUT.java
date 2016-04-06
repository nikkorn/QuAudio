package com.quaudio.quserver.clientmanager;

import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class IncomingActionUT {

	@Test
	public void createIncomingAction() {
		IncomingActionType actionType = IncomingActionType.PLAY;
		JSONObject incomingActionBody = new JSONObject();
		// Manually set the action type in the JSON body of our action.
		incomingActionBody.put("action_type", actionType.toString());
		IncomingAction incomingAction = new IncomingAction(incomingActionBody);
		assertTrue("action type does not match, created action of type'" + actionType + "' but got type '"
				+ incomingAction.getIncomingActionType() + "'", 
				incomingAction.getIncomingActionType() == actionType);
	}

	@Test
	public void createInvalidIncomingAction() {
		// Using the reference we have to the JSONObject action body, lets manually alter 
		// the incoming action type to be something the server will not expect or support. This 
		// is a likely situation if our client and server go out of sync.
		JSONObject incomingActionBody = new JSONObject();
		incomingActionBody.put("action_type", "#_UNSUPPORTED_ACTION_#");
		IncomingAction incomingAction = new IncomingAction(incomingActionBody);
		// When querying the type of our IncomingAction, we should get an 'UNKNOWN' IncomingActionType
		assertTrue("action type should be 'UNKNOWN' but got type '" + incomingAction.getIncomingActionType() + "'",
				incomingAction.getIncomingActionType() == IncomingActionType.UNKNOWN);
	}
}
