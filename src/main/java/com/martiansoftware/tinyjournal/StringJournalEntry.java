package com.martiansoftware.tinyjournal;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * A JournalEntry that encodes data to a single-line String with a CRC.
 * 
 * The format is TIMESTAMP SIZE DATA CRC, where:
 * 
 * <ul>
 *   <li><b>TIMESTAMP</b> = the instant at which the entry is written</li>
 *   <li><b>SIZE</b> = the size of the data, in bytes</li>
 *   <li><b>DATA</b> = the journal entry data, encoded as base64 using the URL and Filename safe Base64 Alphabet</li>
 *   <li><b>CRC</b> = a checksum of the UTF-8 bytes of the encoded line including the timestamp and the space preceding the CRC (and everything in between)</li>
 * </ul>
 * 
 * TIMESTAMP, SIZE, DATA, and CRC are each separated by a single space.
 * 
 * @author <a href="http://martylamb.com">Marty Lamb</a>
 */
class StringJournalEntry implements JournalEntry {
 
    private final String _s;
    private final Instant _timestamp;
    private final byte[] _data;
    
    private static final String TSPATTERN = "[^\\s]+"; // let Instant do the heavy lifting on parsing this
    private static final String LENPATTERN = "[0-9]+";
    private static final String DATAPATTERN = "[A-Za-z0-9-_=]+";
    private static final String CRCPATTERN = "[0-9]+";    
    private static final Pattern p = Pattern.compile(String.format("^(?<crcscope>(?<ts>%s) (?<len>%s) (?<data>%s)? )(?<crc>%s)$", TSPATTERN, LENPATTERN, DATAPATTERN, CRCPATTERN));
    
    /**
     * Creates a new StringJournalEntry referencing (NOT copying!) the supplied byte array as data.
     * @param buf the byte array comprising the data to write to the journal
     */
    public StringJournalEntry(byte[] buf) {
        Objects.requireNonNull(buf, "Journal message must not be null!");
        _timestamp = Instant.now();
        StringBuilder sb = new StringBuilder();
        sb.append(_timestamp);
        sb.append(' ');
        sb.append(buf.length);
        sb.append(' ');
        sb.append(Base64.getUrlEncoder().encodeToString(buf));
        sb.append(' ');
        String s = sb.toString();
        
        _s = String.format("%s%d", s, crc(s));
        _data = buf;
    }
    
    /**
     * Creates a new StringJournalEntry by parsing and validating the supplied String
     * @param s the String read from the journal
     * @throws CorruptedJournalEntryException if any parsing errors occur.  See exception's message for details of error.
     */
    public StringJournalEntry(String s) throws CorruptedJournalEntryException {
        Matcher m = p.matcher(s);
        if (!m.matches()) corrupted("Invalid journal entry \"%s\"", s);        
        long crc = eval(() -> Long.parseLong(m.group("crc")), "Invalid crc: \"%s\"", m.group("crc"));
        if (crc != crc(m.group("crcscope"))) corrupted("Bad CRC: computed %d but read %d", crc(m.group("crcscope")), crc);
        // the below checks are only necessary if the crc is *correct* and the message was simply originally malformed!
        _timestamp = eval(() -> Instant.parse(m.group("ts")),"Invalid timestamp: \"%s\"", m.group("ts")); 
        long len = eval(() -> Long.parseLong(m.group("len")), "Invalid length: \"%s\"", m.group("len"));
        _data = eval(() -> Base64.getUrlDecoder().decode(m.group("data")), "Error parsing data: \"%s\"", m.group("data"));
        if(_data.length != len) corrupted("Data length mismatch: expected %d but found %d.", len, _data.length);
        _s = s;
    }
    
    // quick and dirty helper for concise exception throws
    private void corrupted(String fmt, Object... args) throws CorruptedJournalEntryException {
        throw new CorruptedJournalEntryException(String.format(fmt, args));
    }
    
    // quick and dirty helper for concisely wrapping any exceptions that occur during a computation in a CorruptedJournalEntryException
    private <T> T eval(Supplier<T> expr, String exceptionFormat, Object... args) throws CorruptedJournalEntryException {
        try {
            return expr.get();
        } catch (Throwable t) {
            throw new CorruptedJournalEntryException(String.format(exceptionFormat, args), t);
        }
    }
    
    private long crc(String s) {
        CRC32 crc = new CRC32();
        crc.reset();
        crc.update(s.getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }
    
    @Override public final Instant timestamp() { return _timestamp; }
    @Override public final byte[] bytes() { return _data; }
    @Override public final String toString() { return _s; }
                
}
