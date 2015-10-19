package com.martiansoftware.tinyjournal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A very simple interface for safely writing and reading arbitrary journal entries
 * to/from a backing data store.
 * 
 * @author mlamb
 */
public interface TinyJournal {    
    
    /**
     * Returns the Stream of JournalEntries, silently skipping any corrupted entries.
     * 
     * @return the Stream of JournalEntries
     * @throws IOException if an error occurs while reading the backing data store
     */
    public Stream<JournalEntry> stream() throws IOException;
    
    /**
     * Returns the Stream of JournalEntries, passing any corrupted journal entries
     * to the supplied corruption handler.
     * 
     * @param corruptionHandler if not null, any corrupted journal entries will be passed into this
     * @return the Stream of JournalEntries
     * @throws IOException if an error occurs while reading the backing data store
     */
    public Stream<JournalEntry> stream(Consumer<CorruptedJournalEntryException> corruptionHandler) throws IOException;

    /**
     * Writes a new timestamped JournalEntry containing the supplied data
     * 
     * @param data the data comprising the JournalEntry
     * @throws IOException if an error occurs while writing to the backing data store
     */
    public void write(byte[] data) throws IOException;
    
    /**
     * Convenience method to write a String as a byte array of UTF-8.  To read it back,
     * see JournalEntry.readString().
     * 
     * @param s the String to write.  May not be null;
     * @throws IOException if an error occurs while writing to the backing data store
     */
    public default void writeString(String s) throws IOException {
        write(s.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Closes this TinyJournal and releases any resources is has open.  No more writes are permitted after closing.
     */
    public void close();
}
