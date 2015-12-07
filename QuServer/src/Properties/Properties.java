package Properties;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class Properties {
	private final String PROPERTY_FILE_PATH = "qu.prop.xml";
	private File propertiesXmlFile;
	private Document xmlDoc;
	private boolean propertiesChanged = false;
	
	public Properties() {
		try {
			propertiesXmlFile = new File(PROPERTY_FILE_PATH);
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			xmlDoc = docBuilder.parse(propertiesXmlFile);
		} catch (Exception e) {
			// TODO If we can't get our properties then we can't continue, stop the application.
		}
	}
	
	public synchronized String getDeviceName() {
		Node node = xmlDoc.getDocumentElement().getElementsByTagName("DEVICE_NAME").item(0);
		return node.getTextContent();
	}
	
	public synchronized void setDeviceName(String deviceName) {
		Node node = xmlDoc.getDocumentElement().getElementsByTagName("DEVICE_NAME").item(0);
		node.setTextContent(deviceName);
		propertiesChanged = true;
	}
	
	public synchronized String getDeviceId() {
		Node node = xmlDoc.getDocumentElement().getElementsByTagName("DEVICE_ID").item(0);
		return node.getTextContent();
	}
	
	public synchronized void setDeviceId(String deviceId) {
		Node node = xmlDoc.getDocumentElement().getElementsByTagName("DEVICE_ID").item(0);
		node.setTextContent(deviceId);
		propertiesChanged = true;
	}
	
	public synchronized int getAudioFileReceiverPort() {
		Node node = xmlDoc.getDocumentElement().getElementsByTagName("AFR_PORT").item(0);
		return Integer.parseInt(node.getTextContent());
	}
	
	public synchronized int getClientManagerPort() {
		Node node = xmlDoc.getDocumentElement().getElementsByTagName("CM_PORT").item(0);
		return Integer.parseInt(node.getTextContent());
	}
	
	public synchronized String getUploadDirectory() {
		Node node = xmlDoc.getDocumentElement().getElementsByTagName("TEMP_FILE_DIR").item(0);
		return node.getTextContent();
	}
	
	public synchronized boolean loggingToFileEnabled() {
		Node node = xmlDoc.getDocumentElement().getElementsByTagName("LOG_TO_FILE").item(0);
		return Boolean.parseBoolean(node.getTextContent());
	}
	
	public synchronized boolean loggingToConsoleEnabled() {
		Node node = xmlDoc.getDocumentElement().getElementsByTagName("LOG_TO_CONSOLE").item(0);
		return Boolean.parseBoolean(node.getTextContent());
	}
	
	public synchronized void setUploadDirectory(String uploadDir) {
		Node node = xmlDoc.getDocumentElement().getElementsByTagName("TEMP_FILE_DIR").item(0);
		node.setTextContent(uploadDir);
		propertiesChanged = true;
	}
	
	public synchronized boolean hasChanges() {
		boolean hasChanged = propertiesChanged;
		propertiesChanged = false;
		return hasChanged;
	}
	
	public synchronized void write() {
		try {
			Transformer xmlTransformer = TransformerFactory.newInstance().newTransformer();
			DOMSource propertiesXMLSource = new DOMSource(xmlDoc);
			xmlTransformer.transform(propertiesXMLSource, new StreamResult(propertiesXmlFile));
		} catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
			// TODO If we can't get our properties then we can't continue, stop the application.
		} catch (TransformerException e) {
			// TODO If we can't get our properties then we can't continue, stop the application.
		}
	}
}