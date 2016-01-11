import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 *
 * QuServer Automatic upDater
 * @author Nikolas Howard
 *
 */
public class QuAD {
	private QuADInfo quadInfo;
	
	public static void main(String[] args) {
		new QuAD().update();
	}
	
	/**
	 * Attempt an update of the software
	 */
	public void update() {
		// Create a new QuADInfo object that represents local settings.
		quadInfo = new QuADInfo();
		quadInfo.init();
		
		// Do a HTTP GET for our 'quad-version' file.
		Scanner quadVersionScanner = null;
		try {
			quadVersionScanner = new Scanner(new URL(quadInfo.getTargetURL() + "/quad-version").openStream());
		} catch (IOException e) {
			e.printStackTrace();
			// We failed to get a PrintWriter, QuAD has failed
			System.out.println("QuAD: Error: failed to fetch version info from '" + quadInfo.getTargetURL() + "/quad-version'");
			System.exit(ReturnValue.TARGET_UNREACHABLE.ordinal());
		}
		
		String globalRecentVersion = quadVersionScanner.nextLine();
		globalRecentVersion = globalRecentVersion.split("@")[1];
		
		// Is there a mismatch between the local version and the most up-to-date version? if so then get the latest.
		if(!globalRecentVersion.equals(quadInfo.getVersion())) {
			getLatestVersion(globalRecentVersion);
		} else {
			// We are using the latest version.
			System.out.println("QuAD: already using latest version'" + quadInfo.getVersion() + "'");
			System.exit(ReturnValue.UP_TO_DATE.ordinal());
		}
	}

	/**
	 * Replaces the local outdated version with the latest.
	 * @param globalRecentVersion
	 */
	private void getLatestVersion(String globalRecentVersion) {
		// Create our new 'source-nw' directory that will receive the latest version.
		File oldSourceDir = new File("source");
		File newSourceDir = new File("source-nw");
		newSourceDir.mkdir();
		// Where we will be writing the latest version zip file to.
		File targetSourceFile = new File("quad_src.zip");
		// The location of the target file on the server.
		String target = quadInfo.getTargetURL() + "/" + globalRecentVersion + ".zip";
		// Try to now actually get the physical ZIP file.
		try {
			URL targetSite = new URL(target);
			FileOutputStream fileOutputStream = new FileOutputStream(targetSourceFile);
			fileOutputStream.getChannel().transferFrom(Channels.newChannel(targetSite.openStream()) , 0, Long.MAX_VALUE);
			fileOutputStream.close();
		} catch(IOException ioe) {
			// We failed to get the latest version form server.
			System.out.println("QuAD: Error: failed to fetch '" + target + "' file from server");
			// Clean up mess
			emergencyCleanup();
			System.exit(ReturnValue.TARGET_UNREACHABLE.ordinal());
		}
		// Unzip the latest into 'source-nw'
		try {
	         ZipFile zipFile = new ZipFile("quad_src.zip");
	         zipFile.extractAll("source-nw");
	    } catch (ZipException e) {
	    	// We failed to unzip our latest version! error
			System.out.println("QuAD: Error: failed to unzip latest version");
			// Clean up mess
			emergencyCleanup();
			System.exit(ReturnValue.FAILED.ordinal());
	    }
		// Delete the zip file now that we are done with it.
		new File("quad_src.zip").delete();
		// Copy over files that we have defined in 'quad-persist'
		copyPersistingFiles();
		// Update the local 'quad-info' file with the new version.
		quadInfo.setVersion(globalRecentVersion);
		quadInfo.write();
		// Delete the old source file.
		deleteDir(oldSourceDir);
		// Rename the new source folder to 'source' 
		newSourceDir.renameTo(new File("source"));
		// Everything seemed to go well and we should now have the latest software bin in the 'source' directory
		System.out.println("QuAD: updated to version '" + globalRecentVersion + "'!");
		System.exit(ReturnValue.UPDATED.ordinal());
	}
	
	/**
	 * Copy over files that we have defined in 'quad-persist'
	 */
	public void copyPersistingFiles() {
		Scanner quadPersistScanner = null;
		try {
			quadPersistScanner = new Scanner(new File("quad-persist"));
		} catch (FileNotFoundException e) {
			// We failed to get the latest version form server.
			System.out.println("QuAD: Error: failed to set up scanner for 'quad-persist'");
			// Clean up mess
			emergencyCleanup();
			System.exit(ReturnValue.FAILED.ordinal());
		}
		while(quadPersistScanner.hasNextLine()) {
			String fileToCopy = quadPersistScanner.nextLine();
			File sourceFile = new File("source/" + fileToCopy);
			if(sourceFile.exists()) {
				try {
					File target = new File("source-nw/" + fileToCopy);
					if(!target.getParentFile().exists()) {
						target.getParentFile().mkdirs();
					}
					target.getParentFile().mkdirs();
					FileUtils.copyFileToDirectory(sourceFile, target.getParentFile());
				} catch (IOException e) {
					// We failed to copy across a file the user wants to stay. error
					System.out.println("QuAD: Error: failed to copy persisting file '" + sourceFile.getAbsolutePath() + "'");
					// Clean up mess
					emergencyCleanup();
					System.exit(ReturnValue.FAILED.ordinal());
				}
			}
		}
	}
	
	/**
	 * Delete a directory
	 * @param dir
	 */
	public void deleteDir(File dir) {
		if (dir.isDirectory()) {
			 for(File sub : dir.listFiles()) {
				 deleteDir(sub);
			 }
		} 
		dir.delete();
	}
	
	/**
	 * Cleans up mess on error
	 */
	public void emergencyCleanup() {
		File srcZipFile = new File("quad_src.zip");
		if(srcZipFile.exists()) {
			srcZipFile.delete();
		}
		
		File newSourceFile = new File("source-nw");
		if(newSourceFile.exists()) {
			newSourceFile.delete();
		}
	}
}
