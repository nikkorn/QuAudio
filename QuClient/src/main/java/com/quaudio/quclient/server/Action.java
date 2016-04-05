package com.quaudio.quclient.server;

import org.json.JSONObject;

/**
 * Represents an Incoming/Outgoing Action.
 * @author Nikolas Howard
 *
 */
public class Action {
	protected JSONObject actionInfoObject;

	public JSONObject getActionInfoObject() {
		return actionInfoObject;
	}
}
