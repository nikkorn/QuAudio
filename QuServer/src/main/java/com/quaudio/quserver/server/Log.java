package com.quaudio.quserver.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

/**
 * 
 * @author Nikolas Howard
 *
 */
public class Log {
	private static boolean writeToFile = false;
	private static boolean writeToConsole = false;
	private static Object consoleWriteLock = new Object();
	private static File logFile = null;
	private static PrintWriter logFileWriter = null;
	private static DateFormat dateTimeFileFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
	private static DateFormat dateTimeFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
	
	private static LinkedList<LogEntry> pendingLogEntries = null;
	
	public enum MessageType {
		INFO,
		WARNING,
		ERROR,
		CRITICAL
	}
	
	public static void initialise() {
		// Read server properties to determine whether we are logging to console/file
		writeToConsole = Server.properties.loggingToConsoleEnabled();
		writeToFile = Server.properties.loggingToConsoleEnabled();
		
		// Are we writing log entries to disk?
		if(writeToFile) {
			pendingLogEntries = new LinkedList<LogEntry>();
			// Create a new log file for this session.
			logFile = new File("logs/" + dateTimeFileFormat.format(new Date()));
			if(!logFile.exists()) {
				try {
					logFile.createNewFile();
				} catch (IOException e) {
					// Could not create log file.
					writeToFile = false;
					System.out.println("Failed to create session log file!");
				}
				try {
					logFileWriter = new PrintWriter(new FileWriter(logFile, true));
				} catch (IOException e) {
					// Could not create log file writer.
					writeToFile = false;
					System.out.println("Failed to create session log file writer!");
				}
				if(!writeToFile) {
					System.out.println("Logging to file is disabled!");
				}
			}
		}
	}
	
	public static void log(MessageType messageType, String area, String message) {
		// Are we writing to the console?
		if(writeToConsole) {
			synchronized(consoleWriteLock) {
				String composedLogEntry = dateTimeFormat.format(new Date()) + " : " 
						+ padValue(messageType.toString(), 10) + " : " + padValue(area, 20) + " : " + message;
				System.out.println(composedLogEntry);
			}
		}
		// Are we writing to file? 
		if(writeToFile) {
			LogEntry entry = new LogEntry(messageType, area, message, dateTimeFormat.format(new Date()));
			synchronized(pendingLogEntries) {
				pendingLogEntries.add(entry);
			}
		}
	}
	
	public static void appendLogEntriesToFile() {
		synchronized(pendingLogEntries) {
			// Write all pending log entries to the log file.
			for(LogEntry entry : pendingLogEntries) {
				logFileWriter.println(entry.getComposedEntry());
			}
			logFileWriter.flush();
			// Clear all log entries for the pending log entries list.
			pendingLogEntries.clear();
		}
	}
	
	public static boolean writingToFile() {
		return writeToFile;
	}
	
	/**
	 * Called when we need to write to the log before the server is killed.
	 * Needed as the main server loop wont get a chance to call 'appendLogEntriesToFile()'
	 */
	public static void forceWrite() {
		appendLogEntriesToFile();
	}
	
	public static String padValue(String text, int length) {
		if(text.length() >= length) {
			return text;
		}
		String paddedValue = text;
		for(int i = 0; i < (length - text.length()); i++) {
			paddedValue += " ";
		}
		return paddedValue;
	}
}
