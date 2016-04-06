package com.quaudio.quserver.clientmanager;

import org.junit.Test;
import static org.junit.Assert.*;

import org.json.JSONObject;

public class OutgoingActionUT {
	
    @Test
    public void createOutgoingAction() {
    	OutgoingActionType actionType = OutgoingActionType.PUSH_PLAYLIST;
    	JSONObject outgoingActionBody = new JSONObject();
    	OutgoingAction outgoingAction = new OutgoingAction(actionType, outgoingActionBody);
    	assertTrue("action type does not match, created action of type'" + 
    			actionType + "' but got type '" + outgoingAction.getOutgoingActionType() + "'", 
    			outgoingAction.getOutgoingActionType() == actionType);
    }
}
