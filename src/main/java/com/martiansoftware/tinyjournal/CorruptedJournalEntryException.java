package com.martiansoftware.tinyjournal;

/**
 * Exception thrown when unparseable journal entries are encountered.  See
 * the Exception's message and any provided root cause for details.
 * 
 * @author <a href="http://martylamb.com">Marty Lamb</a>
 */
public class CorruptedJournalEntryException extends Exception {    
    public CorruptedJournalEntryException(String msg) { super(msg); }
    public CorruptedJournalEntryException(String msg, Throwable rootCause) { super(msg, rootCause); }    
}
