package com.martiansoftware.tinyjournal;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * A single entry in the journal.  TinyJournal.stream() will return a Stream of these.
 * @author mlamb
 */
public interface JournalEntry {
    /**
     * The Instant at which the original JournalEntry was created (generally when it was written)
     * @return the Instant at which the original JournalEntry was created
     */
    public Instant timestamp();
    
    /**
     * The JournalEntry's data.  Do with this what you will.
     * @return the JournalEntry's data
     */
    public byte[] bytes();
    
    /**
     * Convenience method to read a JournalEntry as a String.  This method assumes
     * the JournalEntry data consists of a UTF-8-encoded String.  This is a companion
     * method to TinyJournal.writeString()
     * 
     * @return a String representation of the JournalEntry's data
     */
    public default String readString() {
        return new String(bytes(), StandardCharsets.UTF_8);
    }
}
