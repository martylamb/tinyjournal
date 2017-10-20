package com.martiansoftware.tinyjournal;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A simple file-based implementation of TinyJournal.
 * The file consists of StringJournalEntries that are always written with
 * leading and trailing newlines.  Because StringJournalEntries never contain
 * newlines, this guarantees that reading the file line by line will correctly
 * identify journal entry boundaries.
 * 
 * @author <a href="http://martylamb.com">Marty Lamb</a>
 */
public class TinyFileJournal implements TinyJournal {

    private final Path _path;       // path to the backing file
    private final PrintWriter _out; // writes to the backing file
    private boolean _closed = true; // needed because printwriter will fail silently if backing stream is closed
    
    /**
     * Creates a new TinyFileJournal backed by a file at the specified Path.
     * The file will be created if necessary; otherwise the file will be
     * appended to.
     * 
     * @param path the Path to the backing file
     * @throws IOException if unable to open the backing file
     */
    public TinyFileJournal(Path path) throws IOException {
        _path = path;
        _out = new PrintWriter(
                new OutputStreamWriter(
                    new BufferedOutputStream(Files.newOutputStream(path, CREATE, APPEND)),
                    StandardCharsets.UTF_8));
        _closed = false;
    }
    
    @Override public void close() { _out.close(); _closed = true;}

    /**
     * Returns the Stream of JournalEntries, silently skipping any corrupted entries.
     * Note that the implementation uses Files.lines() to read the backing file, so
     * any IOExceptions that occur while reading the stream will be converted into
     * UncheckedIOExceptions.
     * 
     * @return the Stream of JournalEntries
     * @throws IOException if unable to open the backing file for reading
     */
    @Override public Stream<JournalEntry> stream() throws IOException { return stream(null); }

    /**
     * Returns the Stream of JournalEntries, passing any corrupted journal entries
     * to the supplied corruption handler.
     * Note that the implementation uses Files.lines() to read the backing file, so
     * any IOExceptions that occur while reading the stream will be converted into
     * UncheckedIOExceptions.
     * 
     * @param corruptionHandler if not null, any corrupted journal entries will be passed into this
     * @return the Stream of JournalEntries
     * @throws IOException if unable to open the backing file for reading
     */
    @Override public Stream<JournalEntry> stream(Consumer<CorruptedJournalEntryException> corruptionHandler) throws IOException {       
        return Files.lines(_path).filter((s) -> !s.isEmpty()).flatMap((s) -> parseLine(s, corruptionHandler));
    }
    
    private Stream<JournalEntry> parseLine(String line, Consumer<CorruptedJournalEntryException> corruptionHandler) {
        try {
            return Stream.of(new StringJournalEntry(line));
        } catch (CorruptedJournalEntryException e) {
            if (corruptionHandler != null) corruptionHandler.accept(e);
        }
        return Stream.empty();
    }

    @Override public void write(byte[] buf) throws IOException {
        if (_closed) throw new IOException("Journal has been closed.");
        _out.format("\n%s\n", new StringJournalEntry(buf));
        _out.flush();
    }
}
