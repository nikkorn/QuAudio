package ClientManager;

import javax.xml.transform.dom.DOMSource;

public class Action {
	private Client owner;
	private ActionType type;
	private DOMSource info;
	
	public Client getOwner() {
		return owner;
	}
	public void setOwner(Client owner) {
		this.owner = owner;
	}
	public ActionType getType() {
		return type;
	}
	public void setType(ActionType type) {
		this.type = type;
	}
	public DOMSource getInfo() {
		return info;
	}
	public void setInfo(DOMSource info) {
		this.info = info;
	}
}
