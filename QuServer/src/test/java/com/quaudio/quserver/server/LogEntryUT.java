package com.quaudio.quserver.server;

import org.junit.Test;
import com.quaudio.quserver.server.Log.MessageType;
import static org.junit.Assert.*;

public class LogEntryUT {
	
    @Test
    public void createLogEntry() {
	LogEntry testEntry = new LogEntry(MessageType.INFO, "SERVER", "test entry", "2016:01:01 00:00:00");
        assertEquals(testEntry.getComposedEntry().trim(), "2016:01:01 00:00:00 : INFO       : SERVER               : test entry");
    }
	
}
