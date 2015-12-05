package Server;

import Server.Log.MessageType;

/**
 * @author Nikolas Howard
 * Represents a log entry.
 */
public class LogEntry {
	private MessageType type;
	private String area;
	private String message;
	private String dateTime;
	
	public LogEntry(MessageType messageType, String area, String message, String dateTime) {
		this.type = messageType;
		this.area = area;
		this.message = message;
		this.dateTime = dateTime;
	}
	
	public String getComposedEntry() {
		return dateTime + " : " + Log.padValue(type.toString(), 10) + " : " + Log.padValue(area, 20) + " : " + message;
	}
}