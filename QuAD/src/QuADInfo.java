import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class QuADInfo {
	private Scanner quadInfoScanner = null;
	private String version;
	private String targetURL;
	private boolean hasVersionChanged = false;
	
	public void init() {
		// Create scanner for 'quad-info' file.
		try {
			quadInfoScanner = new Scanner(new File("quad-info"));
		} catch (FileNotFoundException e) {
			// We failed to get a Scanner, QuAD has failed
			System.out.println("QuAD: Error: failed to initialise scanner for 'quad-info'");
			System.exit(ReturnValue.FAILED.ordinal());
		}
		
		String rawCurrentVersion = quadInfoScanner.nextLine();
		this.version = rawCurrentVersion.split("@")[1];
		
		String rawTargetURL = quadInfoScanner.nextLine();
		this.targetURL = rawTargetURL.split("@")[1];
	}
	
	/**
	 * Write version info back to 'quad-info'
	 */
	public void write() {
		PrintWriter quadInfoFileWriter = null;
		try {
			quadInfoFileWriter = new PrintWriter(new File("quad-info"));
		} catch (FileNotFoundException e) {
			// We failed to get a PrintWriter, QuAD has failed
			System.out.println("QuAD: Error: failed to initialise printwriter for 'quad-info'");
			System.exit(ReturnValue.FAILED.ordinal());
		}
		
		quadInfoFileWriter.println("CURRENT_VERSION@" + this.getVersion());
		quadInfoFileWriter.println("TARGET_URL@" + this.getTargetURL());
		quadInfoFileWriter.flush();
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		hasVersionChanged = !version.equals(this.getVersion());
		this.version = version;
	}

	public String getTargetURL() {
		return targetURL;
	}
	
	public boolean hasVersionChanged() {
		return this.hasVersionChanged;
	}
}
