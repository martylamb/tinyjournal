/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martiansoftware.tinyjournal;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
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
public class StringJournalEntryTest {
    
    public StringJournalEntryTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test public void testTimestamp() throws CorruptedJournalEntryException {
        StringJournalEntry e = new StringJournalEntry("2015-10-19T00:14:24.341Z 35 VHdvIHJvYWRzIGRpdmVyZ2VkIGluIGEgeWVsbG93IHdvb2Q= 925641221");
        assertEquals(Instant.parse("2015-10-19T00:14:24.341Z"), e.timestamp());
    }

    @Test public void testBytes() throws CorruptedJournalEntryException {
        StringJournalEntry e = new StringJournalEntry("2015-10-19T00:14:24.341Z 35 VHdvIHJvYWRzIGRpdmVyZ2VkIGluIGEgeWVsbG93IHdvb2Q= 925641221");
        assertTrue(Arrays.equals("Two roads diverged in a yellow wood".getBytes(StandardCharsets.UTF_8), e.bytes()));
    }

    @Test public void testCorruptedTimestamp() {
        try {
            StringJournalEntry e = new StringJournalEntry("2015-10-19T00:14:24.341Zoolander 35 VHdvIHJvYWRzIGRpdmVyZ2VkIGluIGEgeWVsbG93IHdvb2Q= 2051095548");
            fail("Expected a CorruptedJournalEntryException!");
        } catch (CorruptedJournalEntryException expected) {
            System.out.println(expected.getMessage());
            assertTrue(expected.getMessage().startsWith("Invalid timestamp:"));
        }
    }
    
    @Test public void testCorruptedData() {
        try {
            StringJournalEntry e = new StringJournalEntry("2015-10-19T00:14:24.341Z 35 =VHdvIHJvYWRzIGRpdmVyZ2VkIGluIGEgeWVsbG93IHdvb2Q= 3976306718"); // early padding
            fail("Expected a CorruptedJournalEntryException!");
        } catch (CorruptedJournalEntryException expected) {
            System.out.println(expected.getMessage());
            assertTrue(expected.getMessage().startsWith("Error parsing data:"));
        }
    }
    
    @Test public void testBadDataLength() {
        try {
            StringJournalEntry e = new StringJournalEntry("2015-10-19T00:14:24.341Z 34 VHdvIHJvYWRzIGRpdmVyZ2VkIGluIGEgeWVsbG93IHdvb2Q= 4245870998"); // data length is actually 35
            fail("Expected a CorruptedJournalEntryException!");
        } catch (CorruptedJournalEntryException expected) {
            assertTrue(expected.getMessage().startsWith("Data length mismatch:"));
        }            
    }
    
    @Test public void testReallyScrewedUpData() {
        try {
            StringJournalEntry e = new StringJournalEntry("asdkjflhakdjsf");
            fail("Expected a CorruptedJournalEntryException!");
        } catch (CorruptedJournalEntryException expected) {
            assertTrue(expected.getMessage().startsWith("Invalid journal entry"));
        }        
    }
}
