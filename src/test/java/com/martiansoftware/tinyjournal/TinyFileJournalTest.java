package com.martiansoftware.tinyjournal;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mlamb
 */
public class TinyFileJournalTest {
    
    private static final List<Path> tempFiles = new java.util.ArrayList<>();
    
    public TinyFileJournalTest() {
    }
    
    @AfterClass public static void tearDownClass() {
        for (Path p : tempFiles) {
            try { Files.deleteIfExists(p); } catch (IOException e) { e.printStackTrace(); }
        }
    }
    
    private Path tmpFile() throws IOException {
        Path p = Files.createTempFile(TinyFileJournalTest.class.getName(), ".test-tmp");
        tempFiles.add(p);
        return p;
    }
    
    @Test public void testSimpleReadsAndWrites() throws Exception {
        TinyFileJournal j = new TinyFileJournal(tmpFile());
        for (String word : "This is a test".split("\\s+")) j.writeString(word);
        assertEquals(4, j.stream().count());
        assertEquals("This", j.stream().map((e) -> e.readString()).findFirst().get());
        assertEquals("test", j.stream().skip(3).map((e) -> e.readString()).findFirst().get());
        assertEquals("This is a test", j.stream().map((e) -> e.readString()).collect(Collectors.joining(" ")));
        j.writeString("Really");
        j.close();
        assertEquals(5, j.stream().count());
    }    
    
    @Test public void testWriteAfterClose() throws Exception {
        TinyFileJournal j = new TinyFileJournal(tmpFile());
        j.writeString("Hello.");
        j.close();
        try {
            j.writeString("Goodbye.");;
            fail("Expected an IOException when writing to a closed journal!");
        } catch (IOException expected) {}
    }
    
    @Test public void testCorruptedEntries() throws Exception {
        Path p = tmpFile();
        List<CorruptedJournalEntryException> exceptions = new java.util.ArrayList<>();
        
        Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8);
        w.write("2015-10-19T00:14:24.341Z 35 VHdvIHJvYWRzIGRpdmVyZ2VkIGluIGEgeWVsbG93IHdvb2Q= 925641221\n");
        w.write("2015-10-19T00:14:24.365Z 32 QW5kIHNvcnJ5IEkgY291bGQgbm90IHRyYXZlbCBib3Ro 2623152121\n"); // CORRUPTED: data length should be 33
        w.write("2015-10-19T00:14:24.366Z 33 QW5kIGJlIG9uZSB0cmF2ZWxlciwgbG9uZyBJIHN0b29k 8770967\n");
        w.write("2015-10-19T00:14:24.367Z 37 QW5kIGxvb2tlZCBkb3duIG9uZSBhcyBmYXIgYXMgSSBjb3VsZA== 1357003472\n");
        w.write("2015-10-19T00:14:24.367Z 36 VG8gd2hlcmUgaXQgYmVudCBpbiB0aGUgdW5kZXJncm93dGg7 3464495713\n"); // CORRUPTED: crc should be 3464495712
        w.write("Not a journal entry at all!");
        w.close();
        
        TinyFileJournal j = new TinyFileJournal(p);
        assertEquals(3, j.stream().count());
        assertEquals("And looked down one as far as I could", j.stream().reduce((previous, current) -> current).get().readString());
        
        j.stream((e) -> exceptions.add(e)).count(); // use count() to exhaust the stream
        assertEquals(3, exceptions.size());
        assertEquals("Bad CRC: computed 4112716956 but read 2623152121", exceptions.get(0).getMessage());
        assertEquals("Bad CRC: computed 3464495712 but read 3464495713", exceptions.get(1).getMessage());
        assertEquals("Invalid journal entry \"Not a journal entry at all!\"", exceptions.get(2).getMessage());
//        exceptions.forEach((e) -> System.out.println(e.getMessage()));
        
    }
//    @Test public void testWritingStuff() throws Exception {
//        TinyFileJournal j = new TinyFileJournal(Paths.get("/home/mlamb/tj.test"));
//        j.writeString("Two roads diverged in a yellow wood");
//        j.writeString("And sorry I could not travel both");
//        j.writeString("And be one traveler, long I stood");
//        j.writeString("And looked down one as far as I could");
//        j.writeString("To where it bent in the undergrowth;");
//        j.close();
//    }
}
